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
 * cache to keep part of the JSON array in memory.
 * <p>
 * When an item is added to the array, it is stored into the cacheBuffered. Then
 * you can use the method {@link #flushCache(int)} or {@link #storeCache()} to
 * flush the cacheBuffered to file.
 * <p>
 * All methods to get items from the JSON array, look for items into the
 * cacheBuffered but also into the JSON file. Depending on the requested items,
 * this class try to reduce the file access number.
 *
 * @param <T> type of JSON array content.
 * @param <K> type of id of JSON array content.
 */
public abstract class JavaJSONArrayToFile<T, K> {

    // Internal constants

    private final Filter<T> NO_FILTER = new Filter<T>() {
        @Override
        public boolean accepted(T o) {
            return true;
        }
    };


    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(JavaJSONArrayToFile.class);
    private final List<T> cacheBuffered;
    private final ObjectMapper jsonMapper;
    private final File jsonFile;
    private final boolean keepInMemory;
    private final Class<T> typeOfT;
    private JsonNode node;
    private int fileCount;       // count in file (for total add cacheBuffered.size())
    private T fileFirst = null;
    private T fileLast = null;


    // Constructor

    /**
     * Create a new instance of JavaJSONArrayToFile.
     * <p>
     * If keepInMemory is true, then the file is read and stored in memory.
     * <p>
     * If keepInMemory is false, then the file is read and stored in memory only
     * when needed.
     *
     * @param jsonFile     the file to store the JSON array.
     * @param typeOfT      the type of JSON array content.
     * @param keepInMemory if true, the file is read and stored in memory.
     * @throws FileException if file is not a JSON array.
     */
    public JavaJSONArrayToFile(File jsonFile, Class<T> typeOfT, boolean keepInMemory) throws FileException {
        if (jsonFile == null)
            throw new IllegalArgumentException("JSON Array File can not be null");
        if (jsonFile.isDirectory())
            throw new IllegalArgumentException("JSON Array File can not be a directory");

        this.cacheBuffered = new ArrayList<>();

        this.jsonMapper = JsonMapper.builder().build();
        this.jsonFile = jsonFile;
        this.keepInMemory = keepInMemory;
        this.typeOfT = typeOfT;

        initCache();
    }


    // Memory mngm

    private void initCache() throws FileException {
        try {
            ArrayNode array = getMainNode();
            fileCount = array.size();
            if (!array.isEmpty()) {
                fileFirst = jsonMapper.readValue(array.get(0).traverse(), typeOfT);
                fileLast = jsonMapper.readValue(array.get(array.size() - 1).traverse(), typeOfT);
            }
            //array = null;
            printMemory_NoGC();
            Runtime.getRuntime().gc();
            printMemory_NoGC();
        } catch (StreamReadException | DatabindException e) {
            throw new FileException("Badly formatted json file", e);
        } catch (IOException e) {
            throw new FileException(String.format("Error reading file %s", jsonFile.getPath()), e);
        }
    }

    private ArrayNode getMainNode() throws FileException {
        // Check if node is already in memory
        if (keepInMemory && node != null) return (ArrayNode) node;

        // Create new node with file content
        JsonNode newNode = null;
        if (jsonFile.exists() && jsonFile.length() > 0) {
            log.debug(String.format("HEAVY OPS: Reading file '%s'...", jsonFile.getPath()));
            long startTime = System.currentTimeMillis();
            try {
                newNode = jsonMapper.readTree(jsonFile);
                long endTime = System.currentTimeMillis();
                log.warn(String.format("HEAVY OPS: File '%s' read in %-2f seconds (%d items)", jsonFile.getPath(), (endTime - startTime) / 1000.0, newNode.size()));
            } catch (IOException e) {
                long endTime = System.currentTimeMillis();
                throw new FileException(String.format("Error reading file '%s' after %-2f seconds", jsonFile.getPath(), (endTime - startTime) / 1000.0), e);
            }
        }

        // Create new array node if file not exists or other problems
        if (newNode == null) newNode = jsonMapper.createArrayNode();

        // If read node is not an array, throw exception
        if (!newNode.isArray())
            throw new FileException(String.format("File '%s' is not a JSON array", jsonFile.getPath()));

        // Save node in memory
        if (keepInMemory) this.node = newNode;

        return (ArrayNode) newNode;
    }

    public void append(T value) {
        synchronized (cacheBuffered) {
            cacheBuffered.add(value);
        }
    }

    public void storeCache() throws FileException {
        flushCache(count());
    }

    /**
     * Flush cacheBuffered items to file.
     *
     * @param count number of items to flush.
     * @return number of items flushed.
     * @throws FileException if error writing file.
     */
    public int flushCache(int count) throws FileException {
        if (cacheBuffered.isEmpty()) return 0;
        if (count <= 0) return 0;

        int countAdded = 0;
        synchronized (cacheBuffered) {
            ArrayNode array = getMainNode();

            // for each item in cacheBuffered, or until countAdded == count
            for (T v : cacheBuffered) {
                array.insertPOJO(0, v);
                countAdded++;
                if (countAdded == count) break;
            }

            // Update internal vars
            fileCount += countAdded;
            if (fileFirst == null) fileFirst = getFirstBuffered();
            fileLast = cacheBuffered.get(countAdded - 1);

            // Write to file
            log.debug(String.format("HEAVY OPS: Writing file '%s'...", jsonFile.getPath()));
            long startTime = System.currentTimeMillis();
            try {
                jsonMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, array);
            } catch (IOException e) {
                long endTime = System.currentTimeMillis();
                throw new FileException(String.format("Error writing file '%s' after %-2f seconds", jsonFile.getPath(), (endTime - startTime) / 1000.0), e);
            }
            long endTime = System.currentTimeMillis();
            //array = null;
            printMemory_NoGC();
            Runtime.getRuntime().gc();
            printMemory_NoGC();
            log.warn(String.format("HEAVY OPS: File '%s' written in %-2f seconds", jsonFile.getPath(), (endTime - startTime) / 1000.0));

            // Remove from cacheBuffered the added items
            cacheBuffered.subList(0, countAdded).clear();
        }
        return countAdded;
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
        if (!cacheBuffered.isEmpty()) return cacheBuffered.get(0);
        return null;
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
        if (!cacheBuffered.isEmpty())
            return cacheBuffered.get(cacheBuffered.size() - 1);
        return null;
    }

    /**
     * @return the last item added in to the file.
     */
    public T getLastFile() {
        return fileLast;
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

    // todo check reverse
    public List<T> filterAll(Filter<T> filter) throws FileException {
        List<T> filtered = new ArrayList<>(filterAllBuffered(filter));
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

        log.debug(String.format("HEAVY OPS: Scanning file '%s' (filterAllFile)...", jsonFile.getPath()));
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
        printMemory_NoGC();
        Runtime.getRuntime().gc();
        printMemory_NoGC();
        log.warn(String.format("HEAVY OPS: File '%s' scanned in %-2f seconds (filterAllFile)", jsonFile.getPath(), (endTime - startTime) / 1000.0));

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

        log.debug(String.format("HEAVY OPS: Scanning file '%s' (filterLatestFile)...", jsonFile.getPath()));
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
        printMemory_NoGC();
        Runtime.getRuntime().gc();
        printMemory_NoGC();
        log.warn(String.format("HEAVY OPS: File '%s' scanned in %-2f seconds (filterLatestFile)", jsonFile.getPath(), (endTime - startTime) / 1000.0));
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

        log.debug(String.format("HEAVY OPS: Scanning file '%s'...", jsonFile.getPath()));
        long startTime = System.currentTimeMillis();
        long ancientCountLoop = ancientCount;
        for (int i = array.size() - 1; i >= 0; i--) {
            JsonNode node = array.get(i);
            T o;
            try {
                o = jsonMapper.readValue(node.traverse(), typeOfT);
            } catch (IOException e) {
                throw new FileException("error reading file", e);
            }

            if (filter.accepted(o)) {
                filtered.add(o);
                if (--ancientCountLoop == 0) break;
            }
        }
        long endTime = System.currentTimeMillis();
        //array = null;
        printMemory_NoGC();
        Runtime.getRuntime().gc();
        printMemory_NoGC();
        log.warn(String.format("HEAVY OPS: File '%s' scanned in %-2f seconds", jsonFile.getPath(), (endTime - startTime) / 1000.0));
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
        if (toId == null || (getLastBuffered() != null && compareItemIds(getItemId(getFirstBuffered()), toId) < 0))
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

        log.debug(String.format("HEAVY OPS: Scanning file '%s' (filterByIdFile)...", jsonFile.getPath()));
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
        printMemory_NoGC();
        Runtime.getRuntime().gc();
        printMemory_NoGC();
        log.warn(String.format("HEAVY OPS: File '%s' scanned in %-2f seconds (filterByIdFile)", jsonFile.getPath(), (endTime - startTime) / 1000.0));

        // todo check reverse
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

        log.debug(String.format("HEAVY OPS: Scanning file '%s' (filterByDateFile)...", jsonFile.getPath()));
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
        printMemory_NoGC();
        Runtime.getRuntime().gc();
        printMemory_NoGC();
        log.warn(String.format("HEAVY OPS: File '%s' scanned in %-2f seconds (filterByDateFile)", jsonFile.getPath(), (endTime - startTime) / 1000.0));

        // todo check reverse
        Collections.reverse(range);
        return range;
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

    int SIZE = 1024; // KBytes
    DecimalFormat formatter = new DecimalFormat("#,###");
    void printMemory_NoGC() {
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
