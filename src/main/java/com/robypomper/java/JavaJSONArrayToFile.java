/*******************************************************************************
 * The John Operating System Project is the collection of software and configurations
 * to generate IoT EcoSystem, like the John Operating System Platform one.
 * Copyright (C) 2021 Roberto Pompermaier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.robypomper.java;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;


/**
 * Class utils to help store and retrieve JSON arrays from files.
 * <p>
 * This class implements also an internal cache to reduce the number of
 * read/write operations on file.
 * <p>
 * This class use the given file reference to store the JSON array. But, in order
 * to reduce the number of read/write operations on file, it also uses an internal
 * cache to keep part of the JSON array in memory. When an item is added to the
 * array, it is stored into the cacheBuffered. Then you can use the method
 * {@link #flushCache()} or {@link #storeCache()} to flush the cacheBuffered
 * to file.<br/>
 * Thanks to the {@link #isKeepInMemory()} property you can decide if the file
 * must be read and stored in memory until the instance is destroyed. Otherwise,
 * the file is read and stored in memory every time is needed. In both cases,
 * the cacheBuffered is always stored in memory. Remember that the file is keep
 * up to date only when the cacheBuffered is flushed to file.
 * <p>
 * All methods to get items from the JSON array, look for items into the
 * cacheBuffered but also into the JSON file. Depending on the requested items,
 * this class try to reduce the file access number.<br/>
 * This class provide various methods to get/filter items from the JSON array.
 * Here the principal methods:
 * <ul>
 *     <li>{@link #getAll()}</li>
 *     <li>{@link #getLatest(long)}</li>
 *     <li>{@link #getAncient(long)}</li>
 *     <li>{@link #getById(Object, Object)}</li>
 *     <li>{@link #getByData(Date, Date)}</li>
 * </ul>
 * For each one of those methods there is a corresponding method that accept a
 * {@link Filter} to filter the returned items. For example:
 * <pre>
 *     getAll() -> filterAll(Filter)
 * </pre>
 * <p>
 * If {@link #isAutoFlushEnabled()} is enabled, then when the number of items
 * in cacheBuffered is greater than this value, then the cacheBuffered is
 * flushed to file.<br/>
 * Otherwise, the cacheBuffered is flushed to file only when
 * {@link #flushCache()} or {@link #storeCache()} are called.
 *
 * @param <T> type of JSON array content.
 * @param <K> type of id of JSON array content.
 */
@SuppressWarnings({"Convert2Lambda", "LombokGetterMayBeUsed", "unused"})
public abstract class JavaJSONArrayToFile<T, K> {

    // Internal constants

    public static final int DEF_MAX_BUFFER_SIZE = 250;
    public static final int DEF_RELEASE_BUFFER_SIZE = 200;
    public static final int DEF_MAX_FILE_SIZE = 10000;
    public static final int DEF_RELEASE_FILE_SIZE = 2000;
    private final Filter<T> NO_FILTER = new Filter<T>() {
        @Override
        public boolean accepted(T o) {
            return true;
        }
    };
    private static final boolean PRINT_TRACK = false;
    private static final boolean PRINT_HEAVY_OPS = false;
    private static final int PRINT_HEAVY_OPS_MIN_COUNT = 10 * 1000;


    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(JavaJSONArrayToFile.class);
    private static final ObjectMapper jsonMapper = JsonMapper.builder().build();
    /**
     * The type of JSON array content.
     */
    private final Class<T> typeOfT;
    /** The observer of this instance. */
    private Observer<T> observer;
    /**
     * The instance's buffer. Where all items are stored before be flushed into the file.
     * <p>
     * Items are added to the end of the list and removed from the beginning.
     */
    private final List<T> cacheBuffered;
    /**
     * If true, the file is read and stored in memory until the instance is destroyed.
     */
    private final boolean keepInMemory;
    /**
     * The main node of the JSON array. It is used only if `keepInMemory` is `true`.
     * <p>
     * Items are added to the beginning of the file and deleted from the end.
     */
    private JsonNode node;
    /**
     * The instance's file. Where all items are stored permanently.
     */
    private final File jsonFile;
    /**
     * Number of items stored into the file.
     */
    private int fileCount;
    /**
     * First file's item (aka the OLDEST item stored).
     */
    private T fileFirst = null;
    /**
     * Last file's item (aka the NEWST item stored).
     */
    private T fileLast = null;
    /**
     * Max number of items that can be stored into the buffer before flush them into the buffer.
     */
    private int maxBufferSize;
    /**
     * Number of items to flush to the file, when the buffer is full.
     */
    private int releaseBufferSize;
    /**
     * Max number of items that can be stored into the file before delete them permanently.
     */
    private int maxFileSize;
    /**
     * Number of items to delete from the file, when the file is full.
     */
    private int releaseFileSize;
    /** The thread that is executing (or executed) the auto flush procedure, if any. */
    private Thread autoThread = null;
    /**
     * Internal flag used to avoid multiple auto flush at the same time.
     */
    private boolean isAutoFlushing = false;
    /**
     * Last exception occurred during auto flush procedure.
     */
    private FileException flushException = null;


    // Observer interface

    public interface Observer<T> {

        /**
         * Called when items are added to the array.
         *
         * @param items the items added.
         */
        void onAdded(List<T> items);

        /**
         * Called when items are flushed to the file.
         *
         * @param items the items flushed.
         * @param auto true if the flush was performed by the auto flush procedure.
         */
        void onFlushed(List<T> items, boolean auto);

        /**
         * Called when items are removed from the file.
         *
         * @param items the items removed.
         * @param auto true if the remove was performed by the auto flush procedure.
         */
        void onRemoved(List<T> items, boolean auto);

    }


    // Constructor

    /**
     * Create a new instance of JavaJSONArrayToFile.
     *
     * @param jsonFile     the file to store the JSON array.
     * @param typeOfT      the type of JSON array content.
     * @param keepInMemory if true, the file is read and stored in memory until
     *                     the instance is destroyed. Otherwise, the file is
     *                     read and stored in memory every time is needed.
     * @throws FileException if file is not a JSON array.
     */
    public JavaJSONArrayToFile(File jsonFile, Class<T> typeOfT, boolean keepInMemory) throws FileException {
        this(jsonFile, typeOfT, keepInMemory, DEF_MAX_BUFFER_SIZE, DEF_RELEASE_BUFFER_SIZE);
    }

    /**
     * Create a new instance of JavaJSONArrayToFile.
     * <p>
     * If keepInMemory is true, then the file is read and stored in memory.
     * <p>
     * If keepInMemory is false, then the file is read and stored in memory only
     * when needed.
     *
     * @param jsonFile          the file to store the JSON array.
     * @param typeOfT           the type of JSON array content.
     * @param keepInMemory      if true, the file is read and stored in memory until
     *                          the instance is destroyed. Otherwise, the file is
     *                          read and stored in memory every time is needed.
     * @param maxBufferSize     the max number of items that can be stored into the buffer.
     * @param releaseBufferSize the number of items that must be flushed to file when
     *                          {@link #isAutoFlushEnabled()} is enabled.
     * @throws FileException if file is not a JSON array.
     */
    public JavaJSONArrayToFile(File jsonFile, Class<T> typeOfT, boolean keepInMemory, int maxBufferSize, int releaseBufferSize) throws FileException {
        this(jsonFile, typeOfT, keepInMemory, maxBufferSize, releaseBufferSize, DEF_MAX_FILE_SIZE, DEF_RELEASE_FILE_SIZE);
    }

    /**
     * Create a new instance of JavaJSONArrayToFile.
     * <p>
     * If keepInMemory is true, then the file is read and stored in memory.
     * <p>
     * If keepInMemory is false, then the file is read and stored in memory only
     * when needed.
     *
     * @param jsonFile          the file to store the JSON array.
     * @param typeOfT           the type of JSON array content.
     * @param keepInMemory      if true, the file is read and stored in memory until
     *                          the instance is destroyed. Otherwise, the file is
     *                          read and stored in memory every time is needed.
     * @param maxBufferSize     the max number of items that can be stored into the buffer.
     * @param releaseBufferSize the number of items that must be flushed to file when
     *                          {@link #isAutoFlushEnabled()} is enabled.
     * @param maxFileSize       the max number of items that can be stored into the file.
     * @param releaseFileSize   the number of items that must be flushed to file when
     *                          {@link #isAutoFlushEnabled()} is enabled.
     * @throws FileException if file is not a JSON array.
     */
    public JavaJSONArrayToFile(File jsonFile, Class<T> typeOfT, boolean keepInMemory, int maxBufferSize, int releaseBufferSize, int maxFileSize, int releaseFileSize) throws FileException {
        this.typeOfT = typeOfT;

        // setup instance buffer
        this.cacheBuffered = new ArrayList<>();
        this.keepInMemory = keepInMemory;
        //this.node = ...initialized into getMainNode(), if needed

        // setup instance file and his fields
        if (jsonFile == null)
            throw new IllegalArgumentException("JSON Array File can not be null");
        if (jsonFile.isDirectory())
            throw new IllegalArgumentException("JSON Array File can not be a directory");
        this.jsonFile = jsonFile;
        ArrayNode array = getMainNode();
        updateFileProperties(array);
        printTrack(String.format("PRE  INIT()         \tB(%2d)      F(%2d)", countBuffered(), countFile()));
        //array = null;
        freeGC();

        // auto-flush params
        if (maxBufferSize < releaseBufferSize)
            throw new IllegalArgumentException("Param maxBufferSize can not be less than releaseBufferSize");
        this.maxBufferSize = maxBufferSize;
        this.releaseBufferSize = releaseBufferSize;
        if (maxFileSize < releaseFileSize)
            throw new IllegalArgumentException("Param maxFileSize can not be less than releaseFileSize");
        this.maxFileSize = maxFileSize;
        this.releaseFileSize = releaseFileSize;
    }


    private void updateFileProperties(ArrayNode array) throws FileException {
        if (array == null || array.isEmpty()) {
            fileCount = 0;
            this.fileFirst = null;
            this.fileLast = null;
            return;
        }

        this.fileCount = array.size();
        try {
            this.fileFirst = jsonMapper.readValue(array.get(array.size() - 1).traverse(), typeOfT);
            this.fileLast = jsonMapper.readValue(array.get(0).traverse(), typeOfT);
        } catch (StreamReadException | DatabindException e) {
            throw new FileException("Badly formatted json file", e);
        } catch (IOException e) {
            throw new FileException(String.format("Error reading file %s", jsonFile.getPath()), e);
        }
    }

    // Static File methods


    /**
     * Read and parse the main node of the given file as JSON array.
     * <p>
     * NB: This method is a heavy operation.
     *
     * @return the main node of the given file as JSON array.
     * @throws FileException if some IO error occurs during file reading.
     */
    private static JsonNode readFile(File jsonFile) throws FileException {
        printHeavyOpsStart(String.format("Reading file '%s'...", jsonFile.getPath()));
        JsonNode newNode;
        long startTime = System.currentTimeMillis();
        try {
            newNode = jsonMapper.readTree(jsonFile);
            long endTime = System.currentTimeMillis();
            printHeavyOpsEnd(String.format("File '%s' read in %-2f seconds (%d items)", jsonFile.getPath(), (endTime - startTime) / 1000.0, newNode.size()), newNode.size());
        } catch (IOException e) {
            long endTime = System.currentTimeMillis();
            throw new FileException(String.format("Error reading file '%s' after %-2f seconds", jsonFile.getPath(), (endTime - startTime) / 1000.0), e);
        }
        return newNode;
    }

    private static int writeFile(File jsonFile, ArrayNode array) throws FileException {
        printHeavyOpsStart(String.format("Writing file '%s'...", jsonFile.getPath()));
        long startTime = System.currentTimeMillis();
        try {
            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, array);
        } catch (IOException e) {
            long endTime = System.currentTimeMillis();
            throw new FileException(String.format("Error writing file '%s' after %-2f seconds", jsonFile.getPath(), (endTime - startTime) / 1000.0), e);
        }
        long endTime = System.currentTimeMillis();
        int writtenCount = array.size();
        //array = null;
        freeGC();
        printHeavyOpsEnd(String.format("File '%s' written in %-2f seconds (%d items)", jsonFile.getPath(), (endTime - startTime) / 1000.0, array.size()), array.size());

        return writtenCount;
    }


    // Memory mngm

    /**
     * Return the main node of the file as JSON array.
     * <p>
     * If the instance is configured to keep the file in memory (`keepInMemory` = `true`),
     * then, after the first read, the file is stored in memory and returned from memory.
     * Otherwise, the file is read every time this method is called.
     *
     * @return the main node of the file as JSON array.
     * @throws FileException if file is not a JSON array.
     */
    private ArrayNode getMainNode() throws FileException {
        // Check if node is already in memory
        if (keepInMemory && node != null) return (ArrayNode) node;

        // Create new node with file content
        JsonNode newNode = null;
        if (jsonFile.exists() && jsonFile.length() > 0)
            newNode = readFile(this.jsonFile);

        // Create new empty array node if file not exists or other problems
        if (newNode == null) newNode = jsonMapper.createArrayNode();

        // If read node is not an array, throw exception
        if (!newNode.isArray())
            throw new FileException(String.format("File '%s' is not a JSON array", jsonFile.getPath()));

        // Save node in memory
        if (keepInMemory) this.node = newNode;

        return (ArrayNode) newNode;
    }

    /**
     * Add an item to the cacheBuffered.
     * <p>
     * The item is added to the cacheBuffered and then, if the
     * {@link #isAutoFlushEnabled()} is enabled, the cacheBuffered is flushed
     * to file.
     *
     * @param value the item to add.
     */
    public void append(T value) {
        synchronized (cacheBuffered) {
            cacheBuffered.add(value);
            printTrack(String.format("     append(T)      \tB(%2d)      F(%2d)", countBuffered(), countFile()));
        }

        // Emit events
        emitOnAdded(Collections.singletonList(value));

        // Auto flush
        tryAutoFlush();
    }

    // Items are added to the end of the list and removed from the beginning.
    // Items are added to the beginning of the file and deleted from the end.
    /**
     * Remove items from the file and then from the cacheBuffered.
     *
     * @param count the number of items to remove.
     * @return the removed items.
     * @throws FileException if some IO error occurs during file reading/writing.
     */
    public List<T> remove(int count) throws FileException {
        List<T> removedItems = new ArrayList<>();

        synchronized (cacheBuffered) {
            // If required, Delete items from cacheBuffered
            if (count > countFile()) {
                int countRemaining = count - countFile();

                if (countRemaining > countBuffered()) {
                    // Delete ALL items from cacheBuffered
                    removedItems.addAll(cacheBuffered);
                    cacheBuffered.clear();
                } else {
                    // Delete items from cacheBuffered
                    for (int i = 0; i < countRemaining; i++) {
                        if (cacheBuffered.isEmpty()) break;
                        T item = cacheBuffered.remove(0);
                        removedItems.add(item);
                    }
                }
            }

            // Delete items from file
            ArrayNode array = getMainNode();
            for (int i = 0; i < count; i++) {
                if (array.isEmpty()) break;;
                JsonNode nodeRemoved = array.remove(array.size() - 1);
                try {
                    removedItems.add(jsonMapper.readValue(nodeRemoved.traverse(), typeOfT));
                } catch (IOException ignore) {}
            }

            // Update file's properties
            updateFileProperties(array);

            // Write JSON array to file
            writeFile(jsonFile, array);
        }

        // Emit events
        if (!removedItems.isEmpty()) emitOnRemoved(removedItems, false);

        return removedItems;
    }

    private void tryAutoFlush() {
        if (isAutoFlushEnabled()
                && (countBuffered() >= maxBufferSize || countFile() >= maxFileSize)
                && !isAutoFlushing) {
            isAutoFlushing = true;
            autoThread = JavaThreads.initAndStart(new Runnable() {
                @Override
                public void run() {
                    synchronized (cacheBuffered) {
                        try {
                            int preBuffer = countBuffered();
                            int preFile = countFile();
                            flushCache();
                            int postBuffer = countBuffered();
                            int postFile = countFile();

                            int flushed = preBuffer - postBuffer;
                            int deleted = preFile - postFile + flushed;

                            log.warn(String.format("AutoFlushed %d items from buffer and %d items deleted from file", flushed, deleted));
                            log.debug(String.format("History buffered %d items on file %d", countBuffered(), countFile()));
                        } catch (FileException e) {
                            log.warn(String.format("Error accessing to the file from autoFlush process (%s).", e.getMessage()), e);
                            flushException = e;
                        } catch (Throwable e) {
                            log.warn(String.format("Unknown exception from autoFlush process (%s).", e.getMessage()), e);
                        }
                        isAutoFlushing = false;
                    }
                }
            }, "FLUSH_JSON_ARRAY", this.toString());
        }
    }

    public void storeCache() throws FileException {
        flushCache(true);
    }

    public void flushCache() throws FileException {
        flushCache(false);
    }

    public void flushCache(boolean flushAll) throws FileException {
        List<T> flushedItems = new ArrayList<>();
        List<T> removedItems = new ArrayList<>();

        synchronized (cacheBuffered) {
            printTrack(String.format("PRE  flushCache(%b) \tB(%2d)      F(%2d)", flushAll, countBuffered(), countFile()));

            if (flushAll && countBuffered() == 0) {
                printTrack(String.format("POST flushCache(true) \tB(%2d)      F(%2d) Nothing to do because flushAll=True and no items on buffer", countBuffered(), countFile()));
                return;
            }
            if (!flushAll && countBuffered() < maxBufferSize && countFile() < maxFileSize){
                printTrack(String.format("POST flushCache(false) \tB(%2d)      F(%2d) Nothing to do because flushAll=False and buffer nor file exceed their sizes", countBuffered(), countFile()));
                return;
            }

            // Get JSON array from file (or memory)
            ArrayNode array = getMainNode();

            // Flush items from cacheBuffered to JSON array
            if (flushAll || countBuffered() >= maxBufferSize) {
                // Items to flush = Actual size - Desired size
                int toFlushCount = flushAll ? countBuffered() : countBuffered() - (maxBufferSize - releaseBufferSize);
                printTrack(String.format("F    flushCache(%b) \tB(%2d) >%2d> F(%2d)", flushAll, countBuffered(), toFlushCount, countFile()));

                // Flush to JSON Array (add to the beginning)
                int flushedCount = 0;
                for (T v : cacheBuffered) {
                    if (flushedCount >= toFlushCount) break;
                    array.insertPOJO(0, v);
                    flushedItems.add(v);
                    flushedCount++;
                }
                cacheBuffered.removeAll(flushedItems);

                // Update file's properties
                updateFileProperties(array);
            }

            // Delete items from JSON Array
            if (countFile() >= maxFileSize) {
                // Items to delete = Actual size - Desired size
                int toDeleteCount = countFile() - (maxFileSize - releaseFileSize);
                printTrack(String.format("D    flushCache(%b) \tB(%2d)      F(%2d) >> %d", flushAll, countBuffered(), countFile(), toDeleteCount));

                // Delete from JSON Array (remove from the end)
                int removedCount = 0;
                for (int i = 0; i < toDeleteCount; i++) {
                    if (array.isEmpty()) break;
                    JsonNode nodeRemoved = array.remove(array.size() - 1);
                    try {
                        removedItems.add(jsonMapper.readValue(nodeRemoved.traverse(), typeOfT));
                    } catch (IOException ignore) {}
                    removedCount++;
                }

                // Update file's properties
                updateFileProperties(array);
            }

            // Write JSON array to file
            int writtenCount = writeFile(jsonFile, array);
            assert fileCount == writtenCount;
            printTrack(String.format("POST flushCache(%b) \tB(%2d)      F(%2d)", flushAll, countBuffered(), countFile()));
        }

        // Emit events
        boolean isAuto = Thread.currentThread() == autoThread;
        if (!flushedItems.isEmpty()) emitOnFlushed(flushedItems, isAuto);
        if (!removedItems.isEmpty()) emitOnRemoved(removedItems, isAuto);
    }


    // Getters and Setters

    /**
     * @return true if the file is read and stored in memory until the instance
     * is destroyed. Otherwise, the file is read and stored in memory
     * every time is needed.
     */
    public boolean isKeepInMemory() {
        return keepInMemory;
    }

    /**
     * When enabled, the AutoFlush feature flushes the cacheBuffered to file
     * when the number of items in cacheBuffered is greater than
     * {@link #getReleaseBufferSize()}.<br/>
     * Otherwise, the cacheBuffered is flushed to file only when
     * {@link #flushCache()} or {@link #storeCache()} are called.
     * <p>
     * <b>NB:</b> The AutoFlush feature is enabled only when the
     * {@link #getReleaseBufferSize()} is greater than `0`.
     *
     * @return true if auto flush is enabled.
     */
    public boolean isAutoFlushEnabled() {
        return releaseBufferSize > 0;
    }

    /**
     * Return the max number of items that can be stored into the buffer.
     * <p>
     * This value is used only if {@link #isAutoFlushEnabled()} is enabled.
     *
     * @return the max number of items that can be stored into the buffer.
     */
    public int getMaxBufferSize() {
        return maxBufferSize;
    }

    /**
     * Set the max number of items that can be stored into the buffer.
     * <p>
     * This value is used only if {@link #isAutoFlushEnabled()} is enabled.
     *
     * @param maxBufferSize the max number of items that can be stored into the
     *                      buffer.
     */
    public void setMaxBufferSize(int maxBufferSize) {
        if (maxBufferSize < releaseBufferSize)
            throw new IllegalArgumentException(String.format("Param maxBufferSize can not be less than releaseBufferSize ('%d' !< '%d')", maxBufferSize, releaseBufferSize));
        this.maxBufferSize = maxBufferSize;
        tryAutoFlush();
    }

    /**
     * Return the number of items that must be flushed to file when
     * {@link #isAutoFlushEnabled()} is enabled.
     *
     * @return the number of items that can must be stored into the file.
     */
    public int getReleaseBufferSize() {
        return releaseBufferSize;
    }

    /**
     * Set the number of items that must be flushed to file when
     * {@link #isAutoFlushEnabled()} is enabled.
     *
     * @param releaseBufferSize the number of items that can must be stored into
     *                          the file.
     */
    public void setReleaseBufferSize(int releaseBufferSize) {
        if (releaseBufferSize > maxBufferSize)
            throw new IllegalArgumentException(String.format("Param releaseBufferSize can not be greater than maxBufferSize ('%d' !> '%d')", releaseBufferSize, maxBufferSize));
        this.releaseBufferSize = releaseBufferSize;
    }

    /**
     * Return the max number of items that can be stored into the file.
     * <p>
     * This value is used only if {@link #isAutoFlushEnabled()} is enabled.
     *
     * @return the max number of items that can be stored into the file.
     */
    public int getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Set the max number of items that can be stored into the file.
     * <p>
     * This value is used only if {@link #isAutoFlushEnabled()} is enabled.
     *
     * @param maxFileSize the max number of items that can be stored into the
     *                    file.
     */
    public void setMaxFileSize(int maxFileSize) {
        if (maxFileSize < releaseFileSize)
            throw new IllegalArgumentException(String.format("Param maxFileSize can not be less than releaseFileSize ('%d' !< '%d')", maxFileSize, releaseFileSize));
        this.maxFileSize = maxFileSize;
        tryAutoFlush();
    }

    /**
     * Return the number of items that must be flushed to file when
     * {@link #isAutoFlushEnabled()} is enabled.
     *
     * @return the number of items that can must be stored into the file.
     */
    public int getReleaseFileSize() {
        return releaseFileSize;
    }

    /**
     * Set the number of items that must be flushed to file when
     * {@link #isAutoFlushEnabled()} is enabled.
     *
     * @param releaseFileSize the number of items that can must be stored into
     *                        the file.
     */
    public void setReleaseFileSize(int releaseFileSize) {
        if (releaseFileSize > maxFileSize)
            throw new IllegalArgumentException(String.format("Param releaseFileSize can not be greater than maxFileSize ('%d' !> '%d')", releaseFileSize, maxFileSize));
        this.releaseFileSize = releaseFileSize;
    }

    /**
     * When the {@link #isAutoFlushEnabled()} is enabled, this class starts
     * a new thread to flush the items. If the flush fails, then the exception
     * is stored into this var.
     * <p>
     * <b>NB:</b> after the exception is read, it is set to null.
     *
     * @return the exception thrown by the flush thread. `null` if no exception
     * was thrown.
     */
    public FileException getLastFlushException() {
        FileException t = flushException;
        flushException = null;
        return t;
    }


    // Counts, firsts and lasts

    /**
     * @return the total number of items in file and in cacheBuffered.
     */
    public int count() {
        return fileCount + cacheBuffered.size();
    }

    /**
     * @return the number of items in cacheBuffered.
     */
    public int countBuffered() {
        return cacheBuffered.size();
    }

    /**
     * @return the number of items in file.
     */
    public int countFile() {
        return fileCount;
    }

    /**
     * Return the first item added, either into the file or in cacheBuffered.
     *
     * @return the first item in file or in cacheBuffered.
     */
    public T getFirst() {
        if (getFirstFile() != null) return getFirstFile();

        return getFirstBuffered();
    }

    /**
     * @return the first item in cacheBuffered.
     */
    public T getFirstBuffered() {
        return cacheBuffered.isEmpty() ? null : cacheBuffered.get(0);
    }

    /**
     * @return the first item added in to the file.
     */
    public T getFirstFile() {
        return fileFirst;
    }

    /**
     * Return the last item added, either into the file or in cacheBuffered.
     *
     * @return the last item in file or in cacheBuffered.
     */
    public T getLast() {
        if (getLastBuffered() != null) return getLastBuffered();

        return getLastFile();
    }

    /**
     * @return the last item in cacheBuffered.
     */
    public T getLastBuffered() {
        return cacheBuffered.isEmpty() ? null : cacheBuffered.get(cacheBuffered.size() - 1);
    }

    /**
     * @return the last item added in to the file.
     */
    public T getLastFile() {
        return fileLast;
    }


    // Observer's event emitters

    public void registerObserver(Observer<T> observer) {
        this.observer = observer;
    }

    private void emitOnAdded(List<T> items) {
        if (observer!=null) observer.onAdded(items);
    }

    private void emitOnFlushed(List<T> items, boolean auto) {
        if (observer!=null) observer.onFlushed(items, auto);
    }

    private void emitOnRemoved(List<T> items, boolean auto) {
        if (observer!=null) observer.onRemoved(items, auto);
    }


    // Getters and Filters
    // getXX -> filterXX

    public List<T> getAll() throws FileException {
        return filterAll(NO_FILTER);
    }

    public List<T> getLatest(long latestCount) throws FileException {
        return filterLatest(NO_FILTER, latestCount);
    }

    public List<T> getAncient(long ancientCount) throws FileException {
        return filterAncient(NO_FILTER, ancientCount);
    }

    public List<T> getById(K fromId, K toId) throws FileException {
        return filterById(NO_FILTER, fromId, toId);
    }

    public List<T> getByData(Date fromDate, Date toDate) throws FileException {
        return filterByDate(NO_FILTER, fromDate, toDate);
    }


    // Getters and Filters: All (filtered)
    // tryXX -> filterXX -> filterXXBuffered, filterXXFile

    public List<T> tryAll(Filter<T> filter) {
        try {
            return filterAll(filter);
        } catch (FileException e) {
            return Collections.emptyList();
        }
    }

    public List<T> filterAll(Filter<T> filter) throws FileException {
        List<T> filtered = new ArrayList<>(filterAllBuffered(filter));
        // TODO check reverse call
        Collections.reverse(filtered);
        filtered.addAll(filterAllFile(filter));
        return filtered;
    }

    private List<T> filterAllBuffered(Filter<T> filter) {
        List<T> filtered = new ArrayList<>();

        for (T o : cacheBuffered) {
            if (filter.accepted(o)) filtered.add(o);
        }

        return filtered;
    }

    private List<T> filterAllFile(Filter<T> filter) throws FileException {
        List<T> filtered = new ArrayList<>();
        ArrayNode array = getMainNode();

        printHeavyOpsStart(String.format("Scanning file '%s' (filterAllFile)...", jsonFile.getPath()));
        long startTime = System.currentTimeMillis();
        for (Iterator<JsonNode> i = array.elements(); i.hasNext(); ) {
            JsonNode node = i.next();
            T o;
            try {
                o = jsonMapper.readValue(node.traverse(), typeOfT);
            } catch (IOException e) {
                long endTime = System.currentTimeMillis();
                throw new FileException(String.format("Error scanning file '%s' after %-2f seconds (filterAllFile)", jsonFile.getPath(), (endTime - startTime) / 1000.0), e);
            }

            if (filter.accepted(o)) filtered.add(o);
        }
        long endTime = System.currentTimeMillis();
        //array = null;
        freeGC();
        printHeavyOpsEnd(String.format("File '%s' scanned in %-2f seconds (filterAllFile) (%d items)", jsonFile.getPath(), (endTime - startTime) / 1000.0, countFile()), countFile());

        return filtered;
    }


    // Getters and Filters: Latest
    // tryXX -> filterXX -> filterXXBuffered, filterXXFile

    public List<T> tryLatest(Filter<T> filter, long latestCount) {
        try {
            return filterLatest(filter, latestCount);
        } catch (FileException e) {
            return Collections.emptyList();
        }
    }

    public List<T> filterLatest(Filter<T> filter, long latestCount) throws FileException {
        List<T> filtered = new ArrayList<>(filterLatestBuffered(filter, latestCount));
        if (filtered.size() < latestCount)
            filtered.addAll(filterLatestFile(filter, latestCount - filtered.size()));
        return filtered;
    }

    private List<T> filterLatestBuffered(Filter<T> filter, long latestCount) {
        List<T> filtered = new ArrayList<>();

        for (ListIterator<T> i = cacheBuffered.listIterator(cacheBuffered.size()); i.hasPrevious(); ) {
            T o = i.previous();
            if (latestCount-- == 0) break;
            if (filter.accepted(o)) filtered.add(o);
        }

        return filtered;
    }

    private List<T> filterLatestFile(Filter<T> filter, long latestCount) throws FileException {
        List<T> filtered = new ArrayList<>();
        ArrayNode array = getMainNode();

        printHeavyOpsStart(String.format("Scanning file '%s' (filterLatestFile)...", jsonFile.getPath()));
        long startTime = System.currentTimeMillis();
        for (Iterator<JsonNode> i = array.elements(); i.hasNext(); ) {
            JsonNode node = i.next();
            T o;
            try {
                o = jsonMapper.readValue(node.traverse(), typeOfT);
            } catch (IOException e) {
                long endTime = System.currentTimeMillis();
                throw new FileException(String.format("Error scanning file '%s' after %-2f seconds (filterLatestFile)", jsonFile.getPath(), (endTime - startTime) / 1000.0), e);
            }

            if (filter.accepted(o)) {
                filtered.add(o);
                if (--latestCount == 0) break;
            }
        }
        long endTime = System.currentTimeMillis();
        //array = null;
        freeGC();
        printHeavyOpsEnd(String.format("File '%s' scanned in %-2f seconds (filterLatestFile) (%d items)", jsonFile.getPath(), (endTime - startTime) / 1000.0, countFile()), countFile());
        return filtered;
    }


    // Getters and Filters: Ancient
    // tryXX -> filterXX -> filterXXFile, filterXXBuffered

    public List<T> tryAncient(Filter<T> filter, long ancientCount) {
        try {
            return filterAncient(filter, ancientCount);
        } catch (FileException e) {
            return Collections.emptyList();
        }
    }

    public List<T> filterAncient(Filter<T> filter, long ancientCount) throws FileException {
        List<T> filtered = new ArrayList<>(filterAncientFile(filter, ancientCount));
        if (filtered.size() < ancientCount)
            filtered.addAll(filterAncientBuffered(filter, ancientCount - filtered.size()));
        return filtered;
    }

    private List<T> filterAncientBuffered(Filter<T> filter, long ancientCount) {
        List<T> filtered = new ArrayList<>();

        for (T o : cacheBuffered) {
            if (ancientCount-- == 0) break;
            if (filter.accepted(o)) filtered.add(o);
        }

        return filtered;
    }

    private List<T> filterAncientFile(Filter<T> filter, long ancientCount) throws FileException {
        List<T> filtered = new ArrayList<>();
        ArrayNode array = getMainNode();

        printHeavyOpsStart(String.format("Scanning file '%s'...", jsonFile.getPath()));
        long startTime = System.currentTimeMillis();
        long ancientCountLoop = ancientCount;
        for (int i = array.size() - 1; i >= 0; i--) {
            JsonNode node = array.get(i);
            T o;
            try {
                o = jsonMapper.readValue(node.traverse(), typeOfT);
            } catch (IOException e) {
                throw new FileException("Error reading file", e);
            }

            if (filter.accepted(o)) {
                filtered.add(o);
                if (--ancientCountLoop == 0) break;
            }
        }
        long endTime = System.currentTimeMillis();
        //array = null;
        freeGC();
        printHeavyOpsEnd(String.format("File '%s' scanned in %-2f seconds (%d items)", jsonFile.getPath(), (endTime - startTime) / 1000.0, countFile()), countFile());
        return filtered;
    }


    // Getters and Filters: ById
    // tryXX -> filterXX -> filterXXBuffered or filterXXFile [+ filterXXBuffered]

    public List<T> tryById(Filter<T> filter, K fromId, K toId) {
        try {
            return filterById(filter, fromId, toId);
        } catch (FileException e) {
            return Collections.emptyList();
        }
    }

    public List<T> filterById(Filter<T> filter, K fromId, K toId) throws FileException {
        if (count() == 0) return new ArrayList<>();

        // If 1stBufElem <= fromId
        if (fromId != null && getFirstBuffered() != null && compareItemIds(getItemId(getFirstBuffered()), fromId) <= 0)
            // Filter only buffered items
            return filterByIdBuffered(filter, fromId, toId);

        // Filter files items
        List<T> filtered = filterByIdFile(filter, fromId, toId);

        // If LastBufElem < toId
        if (toId == null || (getLastBuffered() != null && compareItemIds(getItemId(getFirstBuffered()), toId) <= 0))
            // Filter also buffered items
            filtered.addAll(filterByIdBuffered(filter, fromId, toId));

        return filtered;
    }

    private List<T> filterByIdBuffered(Filter<T> filter, K fromId, K toId) {
        boolean store = fromId == null;
        List<T> range = new ArrayList<>();

        // Until v.id > toId
        for (T v : cacheBuffered) {
            // To exclude fromId, use > instead >=
            if (fromId != null && compareItemIds(getItemId(v), fromId) >= 0)
                store = true;
            if (toId != null && compareItemIds(getItemId(v), toId) > 0) break;
            if (store && filter.accepted(v)) range.add(v);
        }

        return range;
    }

    private List<T> filterByIdFile(Filter<T> filter, K fromId, K toId) throws FileException {
        boolean store = toId == null;
        List<T> range = new ArrayList<>();
        ArrayNode array = getMainNode();

        printHeavyOpsStart(String.format("Scanning file '%s' (filterByIdFile)...", jsonFile.getPath()));
        long startTime = System.currentTimeMillis();
        // Until v.id < fromId
        for (Iterator<JsonNode> i = array.elements(); i.hasNext(); ) {
            JsonNode node = i.next();
            T o;
            try {
                o = jsonMapper.readValue(node.traverse(), typeOfT);
            } catch (IOException e) {
                long endTime = System.currentTimeMillis();
                throw new FileException(String.format("Error scanning file '%s' after %-2f seconds (filterByIdFile)", jsonFile.getPath(), (endTime - startTime) / 1000.0), e);
            }

            // to exclude toID, use < instead <=
            if (toId != null && compareItemIds(getItemId(o), toId) <= 0)
                store = true;
            if (fromId != null && compareItemIds(getItemId(o), fromId) < 0)
                break;
            if (store && filter.accepted(o)) range.add(o);
        }
        long endTime = System.currentTimeMillis();
        //array = null;
        freeGC();
        printHeavyOpsEnd(String.format("File '%s' scanned in %-2f seconds (filterByIdFile) (%d items)", jsonFile.getPath(), (endTime - startTime) / 1000.0, countFile()), countFile());

        // TODO check reverse call
        Collections.reverse(range);
        return range;
    }


    // Getters and Filters: ByDate
    // tryXX -> filterXX -> filterXXBuffered or filterXXFile [+ filterXXBuffered]

    public List<T> tryByDate(Filter<T> filter, Date fromDate, Date toDate) {
        try {
            return filterByDate(filter, fromDate, toDate);
        } catch (FileException e) {
            return Collections.emptyList();
        }
    }

    public List<T> filterByDate(Filter<T> filter, Date fromDate, Date toDate) throws FileException {
        if (count() == 0) return new ArrayList<>();

        List<T> filtered = filterByDateBuffered(filter, fromDate, toDate);
        filtered.addAll(filterByDateFile(filter, fromDate, toDate));
        return filtered;
    }

    public List<T> filterByDateORIGINAL(Filter<T> filter, Date fromDate, Date toDate) throws FileException {
        if (count() == 0) return new ArrayList<>();

        // If 1stBufElem <= fromDate
        if (fromDate != null && getFirstBuffered() != null && compareItemDate(getItemDate(getFirstBuffered()), fromDate) <= 0)
            // Filter only buffered items
            return filterByDateBuffered(filter, fromDate, toDate);

        // Filter files items
        List<T> filtered = filterByDateFile(filter, fromDate, toDate);

        // If LastBufElem < toDate
        if (toDate == null || (getLastBuffered() != null && compareItemDate(getItemDate(getLastBuffered()), toDate) < 0))
            // Filter also buffered items
            filtered.addAll(filterByDateBuffered(filter, fromDate, toDate));

        return filtered;
    }

    private List<T> filterByDateBuffered(Filter<T> filter, Date fromDate, Date toDate) {
        boolean store = fromDate == null;
        List<T> range = new ArrayList<>();

        // Until v.date > toDate
        for (T v : cacheBuffered) {
            // To exclude fromDate, use > instead >=
            if (fromDate != null && compareItemDate(getItemDate(v), fromDate) >= 0)
                store = true;
            if (toDate != null && compareItemDate(getItemDate(v), toDate) > 0)
                break;
            if (store && filter.accepted(v)) range.add(v);
        }

        return range;
    }

    private List<T> filterByDateFile(Filter<T> filter, Date fromDate, Date toDate) throws FileException {
        boolean store = toDate == null;
        List<T> range = new ArrayList<>();
        ArrayNode array = getMainNode();

        printHeavyOpsStart(String.format("Scanning file '%s' (filterByDateFile)...", jsonFile.getPath()));
        long startTime = System.currentTimeMillis();
        // Until v.date < fromDate
        for (Iterator<JsonNode> i = array.elements(); i.hasNext(); ) {
            JsonNode node = i.next();
            T o;
            try {
                o = jsonMapper.readValue(node.traverse(), typeOfT);
            } catch (IOException e) {
                long endTime = System.currentTimeMillis();
                throw new FileException(String.format("Error scanning file '%s' after %-2f seconds (filterByDateFile)", jsonFile.getPath(), (endTime - startTime) / 1000.0), e);
            }

            // to exclude toDate, use < instead <=
            if (toDate != null && compareItemDate(getItemDate(o), toDate) <= 0)
                store = true;
            if (fromDate != null && compareItemDate(getItemDate(o), fromDate) < 0)
                break;
            if (store && filter.accepted(o)) range.add(o);
        }
        long endTime = System.currentTimeMillis();
        //array = null;
        freeGC();
        printHeavyOpsEnd(String.format("File '%s' scanned in %-2f seconds (filterByDateFile) (%d items)", jsonFile.getPath(), (endTime - startTime) / 1000.0, countFile()), countFile());

        // TODO check reverse call
        Collections.reverse(range);
        return range;
    }


    // Utils

    private static void printTrack(String msg) {
        if (PRINT_TRACK) log.warn("TRACK_JSONArrayToFile " + msg);
    }

    private static void printHeavyOpsStart(String msg) {
        if (PRINT_HEAVY_OPS) log.warn("HEAVY OPS: " + msg);
    }

    private static void printHeavyOpsEnd(String msg, int count) {
        if (PRINT_HEAVY_OPS && count > PRINT_HEAVY_OPS_MIN_COUNT) log.warn("HEAVY OPS: " + msg);
    }

    // Sub class utils

    protected abstract int compareItemIds(K id1, K id2);

    protected abstract K getItemId(T value);

    protected int compareItemDate(Date id1, Date id2) {
        return id1.compareTo(id2);
    }

    protected abstract Date getItemDate(T value);

    public interface Filter<T> {
        boolean accepted(T o);
    }


    public static class FileException extends IOException {

        public FileException(String detailMessage) {
            super(detailMessage);
        }

        public FileException(String detailMessage, Throwable cause) {
            super(detailMessage, cause);
        }

    }


    // Static GC methods

    private static void freeGC() {
        printMemory_NoGC();
        Runtime.getRuntime().gc();
        printMemory_NoGC();
    }

    private static void printMemory_NoGC() {
        int SIZE = 1024; // KBytes
        DecimalFormat formatter = new DecimalFormat("#,###");

        long tot_mem = Runtime.getRuntime().totalMemory() / SIZE;
        long free_mem = Runtime.getRuntime().freeMemory() / SIZE;
        long used_mem = tot_mem - free_mem;
        long max_mem = Runtime.getRuntime().maxMemory() / SIZE;

        log.debug("HEAVY OPS: Tot: " + formatter.format(tot_mem) +
                "\tFree: " + formatter.format(free_mem) +
                "\tUsed: " + formatter.format(used_mem) +
                "\tMax: " + formatter.format(max_mem)
        );
    }

}
