package com.robypomper.java;

import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


// TODO remove
// TODO Observer
class JavaJSONArrayToFileTest {

    private static final boolean KEEP_IN_MEMORY = true;

    public static class TestJavaJSONArrayToFile extends JavaJSONArrayToFile<String, Integer> {

        public TestJavaJSONArrayToFile(File jsonFile, boolean keepInMemory) throws FileException {
            super(jsonFile, String.class, keepInMemory);
        }

        @Override
        protected int compareItemIds(Integer id1, Integer id2) {
            return id1.compareTo(id2);
        }

        @Override
        protected Integer getItemId(String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Date getItemDate(String value) {
            throw new UnsupportedOperationException();
        }
    }

    static class TestData {

        private final long id;
        private final Date updatedAt;
        private final String payload;

        public TestData() {
            this.id = 0;
            this.updatedAt = new Date();
            this.payload = "";
        }
        public TestData(long id, Date updatedAt, String payload) {
            this.id = id;
            this.updatedAt = updatedAt;
            this.payload = payload;
        }


        // Getters

        public long getId() {
            return id;
        }

        public Date getUpdatedAt() {
            return updatedAt;
        }

        public String getPayload() {
            return payload;
        }

    }

    static class TestDataJavaJSONArrayToFile extends JavaJSONArrayToFile<TestData, Long> {

        public TestDataJavaJSONArrayToFile(File jsonFile, boolean keepInMemory) throws FileException {
            super(jsonFile, TestData.class, keepInMemory);
        }

        @Override
        protected int compareItemIds(Long id1, Long id2) {
            return id1.compareTo(id2);
        }

        @Override
        protected Long getItemId(TestData value) {
            return value.getId();
        }

        @Override
        protected Date getItemDate(TestData value) {
            return value.getUpdatedAt();
        }
    }

    @Test
    void testConstructor() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        assertNotNull(arrayToFile);
    }

    @Test
    void testConstructor_IllegalJSONFileAsNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new TestJavaJSONArrayToFile(null, KEEP_IN_MEMORY);
        });
        String expectedMessage = "JSON Array File can not be null";
        String actualMessage = exception.getMessage();
        assertEquals(actualMessage, expectedMessage);
    }

    @Test
    void testConstructor_IllegalJSONFileAsDir() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json").getParentFile();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        });
        String expectedMessage = "JSON Array File can not be a directory";
        String actualMessage = exception.getMessage();
        assertEquals(actualMessage, expectedMessage);
    }

    @Test
    void testConstructor_EmptyFile() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");

        BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));
        writer.write("");
        writer.close();

        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        assertNotNull(arrayToFile);
        assertEquals(0, arrayToFile.count());
        assertEquals(0, arrayToFile.countBuffered());
        assertEquals(0, arrayToFile.countFile());
    }

    @Test
    void testConstructor_EmptyArray() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");

        BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));
        writer.write("[]");
        writer.close();

        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        assertNotNull(arrayToFile);
        assertEquals(0, arrayToFile.count());
        assertEquals(0, arrayToFile.countBuffered());
        assertEquals(0, arrayToFile.countFile());
    }

    @Test
    void testConstructor_Array5Items() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");

        BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));
        writer.write("[\"1\",\"2\",\"3\",\"4\",\"5\"]");
        writer.close();

        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        assertNotNull(arrayToFile);
        assertEquals(5, arrayToFile.count());
        assertEquals(0, arrayToFile.countBuffered());
        assertEquals(5, arrayToFile.countFile());
    }

    @Test
    void testConstructor_FileExceptionNotAnArray() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");

        BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));
        writer.write("\"1\",\"2\",\"3\",\"4\",\"5\"");
        writer.close();

        Exception exception = assertThrows(JavaJSONArrayToFile.FileException.class, () -> {
            new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        });
        String expectedPartialMessage = "is not a JSON array";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedPartialMessage));
    }

    @Test
    void testConstructor_FileExceptionBadFormatted() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");

        BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));
        writer.write("[\"1\",{\"2\"},\"3\",\"4\",\"5\"]");
        writer.close();

        Exception exception = assertThrows(JavaJSONArrayToFile.FileException.class, () -> {
            new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        });
        String expectedPartialMessage = "Error reading file";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedPartialMessage));
    }


    @Test
    void testAppend() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test");
        assertEquals(1, arrayToFile.count());
    }

    @Test
    void testAppend_FileWriteFail() throws IOException {
        //File jsonFile = File.createTempFile("data_empty", ".json");
        File jsonFile = new File("/root/test.json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test");

        Exception exception = assertThrows(JavaJSONArrayToFile.FileException.class, () -> {
            arrayToFile.storeCache();
        });
        String expectedPartialMessage = "Error writing file";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedPartialMessage));
    }


    @Test
    void testRemove() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        arrayToFile.append("test4");
        arrayToFile.append("test5");
        assertEquals(5, arrayToFile.count());
        assertEquals("test1", arrayToFile.getFirst());

        List<String> removed = arrayToFile.remove(1);
        assertEquals(1, removed.size());

        assertEquals(4, arrayToFile.count());
        assertEquals("test2", arrayToFile.getFirst());
    }

    @Test
    void testRemove_OnlyFilePartial() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        arrayToFile.append("test4");

        // 2 items on buffer, 2 items on file
        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(4);
        arrayToFile.flushCache();

        assertEquals(4, arrayToFile.count());

        assertEquals("test1", arrayToFile.getFirst());
        assertEquals("test3", arrayToFile.getFirstBuffered());
        assertEquals("test1", arrayToFile.getFirstFile());

        assertEquals("test4", arrayToFile.getLast());
        assertEquals("test4", arrayToFile.getLastBuffered());
        assertEquals("test2", arrayToFile.getLastFile());

        List<String> removed = arrayToFile.remove(1);
        assertEquals(1, removed.size());

        assertEquals(4-1, arrayToFile.count());

        assertEquals("test2", arrayToFile.getFirst());
        assertEquals("test3", arrayToFile.getFirstBuffered());
        assertEquals("test2", arrayToFile.getFirstFile());

        assertEquals("test4", arrayToFile.getLast());
        assertEquals("test4", arrayToFile.getLastBuffered());
        assertEquals("test2", arrayToFile.getLastFile());
    }

    @Test
    void testRemove_OnlyFileFull() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        arrayToFile.append("test4");

        // 2 items on buffer, 2 items on file
        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(4);
        arrayToFile.flushCache();

        assertEquals(4, arrayToFile.count());

        assertEquals("test1", arrayToFile.getFirst());
        assertEquals("test3", arrayToFile.getFirstBuffered());
        assertEquals("test1", arrayToFile.getFirstFile());

        assertEquals("test4", arrayToFile.getLast());
        assertEquals("test4", arrayToFile.getLastBuffered());
        assertEquals("test2", arrayToFile.getLastFile());

        List<String> removed = arrayToFile.remove(2);
        assertEquals(2, removed.size());

        assertEquals(4-2, arrayToFile.count());

        assertEquals("test3", arrayToFile.getFirst());
        assertEquals("test3", arrayToFile.getFirstBuffered());
        assertNull(arrayToFile.getFirstFile());

        assertEquals("test4", arrayToFile.getLast());
        assertEquals("test4", arrayToFile.getLastBuffered());
        assertNull(arrayToFile.getLastFile());
    }

    @Test
    void testRemove_OnlyBufferPartial() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        arrayToFile.append("test4");

        // 4 items on buffer, 0 items on file
        // N/A

        assertEquals(4, arrayToFile.count());

        assertEquals("test1", arrayToFile.getFirst());
        assertEquals("test1", arrayToFile.getFirstBuffered());
        assertNull(arrayToFile.getFirstFile());

        assertEquals("test4", arrayToFile.getLast());
        assertEquals("test4", arrayToFile.getLastBuffered());
        assertNull(arrayToFile.getLastFile());

        List<String> removed = arrayToFile.remove(1);
        assertEquals(1, removed.size());

        assertEquals(4-1, arrayToFile.count());

        assertEquals("test2", arrayToFile.getFirst());
        assertEquals("test2", arrayToFile.getFirstBuffered());
        assertNull(arrayToFile.getFirstFile());

        assertEquals("test4", arrayToFile.getLast());
        assertEquals("test4", arrayToFile.getLastBuffered());
        assertNull(arrayToFile.getLastFile());
    }

    @Test
    void testRemove_OnlyBufferFull() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        arrayToFile.append("test4");

        // 4 items on buffer, 0 items on file
        // N/A

        assertEquals(4, arrayToFile.count());

        assertEquals("test1", arrayToFile.getFirst());
        assertEquals("test1", arrayToFile.getFirstBuffered());
        assertNull(arrayToFile.getFirstFile());

        assertEquals("test4", arrayToFile.getLast());
        assertEquals("test4", arrayToFile.getLastBuffered());
        assertNull(arrayToFile.getLastFile());

        List<String> removed = arrayToFile.remove(4);
        assertEquals(4, removed.size());

        assertEquals(0, arrayToFile.count());

        assertNull(arrayToFile.getFirst());
        assertNull(arrayToFile.getFirstBuffered());
        assertNull(arrayToFile.getFirstFile());

        assertNull(arrayToFile.getLast());
        assertNull(arrayToFile.getLastBuffered());
        assertNull(arrayToFile.getLastFile());
    }

    @Test
    void testRemove_MixedPartial() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        arrayToFile.append("test4");

        // 2 items on buffer, 2 items on file
        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(4);
        arrayToFile.flushCache();

        assertEquals(4, arrayToFile.count());

        assertEquals("test1", arrayToFile.getFirst());
        assertEquals("test3", arrayToFile.getFirstBuffered());
        assertEquals("test1", arrayToFile.getFirstFile());

        assertEquals("test4", arrayToFile.getLast());
        assertEquals("test4", arrayToFile.getLastBuffered());
        assertEquals("test2", arrayToFile.getLastFile());

        List<String> removed = arrayToFile.remove(3);
        assertEquals(3, removed.size());

        assertEquals(4-3, arrayToFile.count());

        assertEquals("test4", arrayToFile.getFirst());
        assertEquals("test4", arrayToFile.getFirstBuffered());
        assertNull(arrayToFile.getFirstFile());

        assertEquals("test4", arrayToFile.getLast());
        assertEquals("test4", arrayToFile.getLastBuffered());
        assertNull(arrayToFile.getLastFile());
    }

    @Test
    void testRemove_MixedFull() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        arrayToFile.append("test4");

        // 2 items on buffer, 2 items on file
        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(4);
        arrayToFile.flushCache();

        assertEquals(4, arrayToFile.count());

        assertEquals("test1", arrayToFile.getFirst());
        assertEquals("test3", arrayToFile.getFirstBuffered());
        assertEquals("test1", arrayToFile.getFirstFile());

        assertEquals("test4", arrayToFile.getLast());
        assertEquals("test4", arrayToFile.getLastBuffered());
        assertEquals("test2", arrayToFile.getLastFile());

        List<String> removed = arrayToFile.remove(4);
        assertEquals(4, removed.size());

        assertEquals(0, arrayToFile.count());

        assertNull(arrayToFile.getFirst());
        assertNull(arrayToFile.getFirstBuffered());
        assertNull(arrayToFile.getFirstFile());

        assertNull(arrayToFile.getLast());
        assertNull(arrayToFile.getLastBuffered());
        assertNull(arrayToFile.getLastFile());
    }

    @Test
    void testRemove_More() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        arrayToFile.append("test4");

        // 2 items on buffer, 2 items on file
        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(4);
        arrayToFile.flushCache();

        assertEquals(4, arrayToFile.count());

        assertEquals("test1", arrayToFile.getFirst());
        assertEquals("test3", arrayToFile.getFirstBuffered());
        assertEquals("test1", arrayToFile.getFirstFile());

        assertEquals("test4", arrayToFile.getLast());
        assertEquals("test4", arrayToFile.getLastBuffered());
        assertEquals("test2", arrayToFile.getLastFile());

        List<String> removed = arrayToFile.remove(5);
        assertEquals(4, removed.size());

        assertEquals(0, arrayToFile.count());

        assertNull(arrayToFile.getFirst());
        assertNull(arrayToFile.getFirstBuffered());
        assertNull(arrayToFile.getFirstFile());

        assertNull(arrayToFile.getLast());
        assertNull(arrayToFile.getLastBuffered());
        assertNull(arrayToFile.getLastFile());
    }


    @Test
    void testAutoFlush() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(5);

        for (int i = 0; i < 6; i++)
            arrayToFile.append("test" + i);

        JavaThreads.softSleep(100); // let the thread flush the buffer

        assertEquals(5 - 2, arrayToFile.countBuffered());
        assertEquals(3, arrayToFile.countFile());
    }

    @Test
    void testAutoDelete() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        arrayToFile.setReleaseBufferSize(3);
        arrayToFile.setMaxBufferSize(10);
        arrayToFile.setReleaseFileSize(5);
        arrayToFile.setMaxFileSize(20);

        for (int i = 0; i < 50; i++) {
            arrayToFile.append("test" + i);
            arrayToFile.flushCache();
            System.out.printf("%-2d#  TOT %3d => Buffer: %3d File: %3d", i, arrayToFile.count(), arrayToFile.countBuffered(), arrayToFile.countFile());
            System.out.println("\t\t" + arrayToFile.getAll());
        }
    }

    @Test
    void testAutoFlush_becauseSetMaxBufferSize() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        for (int i = 0; i < 6; i++)
            arrayToFile.append("test" + i);

        JavaThreads.softSleep(100); // let the thread flush the buffer

        assertEquals(6, arrayToFile.countBuffered());
        assertEquals(0, arrayToFile.countFile());

        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(5);

        JavaThreads.softSleep(100); // let the thread flush the buffer

        assertEquals(5 - 2, arrayToFile.countBuffered());
        assertEquals(3, arrayToFile.countFile());
    }

    @Test
    void testAutoDelete_becauseSetMaxFileSize() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        for (int i = 0; i < 6; i++)
            arrayToFile.append("test" + i);

        JavaThreads.softSleep(100); // let the thread flush the buffer

        // Buffer: | 5 | 4 | 3 | 2 | 1 | 0 |
        // File:   --
        assertEquals(6, arrayToFile.countBuffered());
        assertEquals(0, arrayToFile.countFile());

        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(5);

        JavaThreads.softSleep(100); // let the thread flush the buffer

        // Buffer: | 5 | 4 | 3 |
        // File:   | 2 | 1 | 0 |
        assertEquals(5 - 2, arrayToFile.countBuffered());
        assertEquals(3, arrayToFile.countFile());

        arrayToFile.setReleaseFileSize(1);
        arrayToFile.setMaxFileSize(2);

        JavaThreads.softSleep(1000); // let the thread flush the buffer and the file

        // Buffer: | 5 | 4 | 3 |
        // File:   | 2 |
        assertEquals(3, arrayToFile.countBuffered());
        assertEquals(1, arrayToFile.countFile());
    }

    @Test
    void testSetMaxBufferSize_IllegalArgumentException() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        arrayToFile.setMaxBufferSize(arrayToFile.getReleaseBufferSize());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            arrayToFile.setMaxBufferSize(arrayToFile.getReleaseBufferSize() - 1);
            arrayToFile.setReleaseBufferSize(10);
        });

        String expectedPartialMessage = "Param maxBufferSize can not be less than releaseBufferSize";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedPartialMessage));
    }

    @Test
    void testSetReleaseBufferSize_IllegalArgumentException() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        arrayToFile.setReleaseBufferSize(arrayToFile.getMaxBufferSize());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            arrayToFile.setReleaseBufferSize(arrayToFile.getMaxBufferSize() + 1);
            arrayToFile.setReleaseBufferSize(10);
        });

        String expectedPartialMessage = "Param releaseBufferSize can not be greater than maxBufferSize";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedPartialMessage));
    }

    @Test
    void testSetMaxFileSize_IllegalArgumentException() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        arrayToFile.setMaxFileSize(arrayToFile.getReleaseFileSize());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            arrayToFile.setMaxFileSize(arrayToFile.getReleaseFileSize() - 1);
            arrayToFile.setReleaseFileSize(10);
        });

        String expectedPartialMessage = "Param maxFileSize can not be less than releaseFileSize";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedPartialMessage));
    }

    @Test
    void testSetReleaseFileSize_IllegalArgumentException() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        arrayToFile.setReleaseFileSize(arrayToFile.getMaxFileSize());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            arrayToFile.setReleaseFileSize(arrayToFile.getMaxFileSize() + 1);
            arrayToFile.setReleaseFileSize(10);
        });

        String expectedPartialMessage = "Param releaseFileSize can not be greater than maxFileSize";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedPartialMessage));
    }


    @Test
    void testStorage_FlushFull() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test");

        assertEquals(1, arrayToFile.count());
        assertEquals(1, arrayToFile.countBuffered());
        assertEquals(0, arrayToFile.countFile());

        arrayToFile.flushCache(true);

        assertEquals(1, arrayToFile.count());
        assertEquals(0, arrayToFile.countBuffered());
        assertEquals(1, arrayToFile.count());
    }

    @Test
    void testStorage_FlushPartial() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        arrayToFile.append("test4");
        arrayToFile.append("test5");

        assertEquals(5, arrayToFile.count());
        assertEquals(5, arrayToFile.countBuffered());
        assertEquals(0, arrayToFile.countFile());

        arrayToFile.setReleaseBufferSize(3);
        arrayToFile.setMaxBufferSize(arrayToFile.countBuffered());
        arrayToFile.flushCache();

        assertEquals(5, arrayToFile.count());
        assertEquals(2, arrayToFile.countBuffered());
        assertEquals(3, arrayToFile.countFile());
    }

    @Test
    void testStorage_StoreSingleItem() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test");

        assertEquals(1, arrayToFile.count());
        assertEquals(1, arrayToFile.countBuffered());
        assertEquals(0, arrayToFile.countFile());

        arrayToFile.storeCache();

        assertEquals(1, arrayToFile.count());
        assertEquals(0, arrayToFile.countBuffered());
        assertEquals(1, arrayToFile.count());
    }

    @Test
    void testStorage_StoreManyItems() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        arrayToFile.append("test4");
        arrayToFile.append("test5");

        assertEquals(5, arrayToFile.count());
        assertEquals(5, arrayToFile.countBuffered());
        assertEquals(0, arrayToFile.countFile());

        arrayToFile.storeCache();

        assertEquals(5, arrayToFile.count());
        assertEquals(0, arrayToFile.countBuffered());
        assertEquals(5, arrayToFile.count());
    }

    @Test
    void testStorage_StorePartial() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        arrayToFile.append("test4");
        arrayToFile.append("test5");
        arrayToFile.setReleaseBufferSize(3);
        arrayToFile.setMaxBufferSize(arrayToFile.countBuffered());
        arrayToFile.flushCache();

        assertEquals(5, arrayToFile.count());
        assertEquals(2, arrayToFile.countBuffered());
        assertEquals(3, arrayToFile.countFile());

        arrayToFile.storeCache();

        assertEquals(5, arrayToFile.count());
        assertEquals(0, arrayToFile.countBuffered());
        assertEquals(5, arrayToFile.count());
    }


    @Test
    void testObserver() throws IOException, InterruptedException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);
        CountDownLatch latchAdded = new CountDownLatch(1);
        CountDownLatch latchFlushed = new CountDownLatch(1);
        CountDownLatch latchRemoved = new CountDownLatch(1);
        arrayToFile.registerObserver(new JavaJSONArrayToFile.Observer<String>() {
            @Override
            public void onAdded(List<String> items) {
                latchAdded.countDown();
            }

            @Override
            public void onFlushed(List<String> items, boolean auto) {
                latchFlushed.countDown();
            }

            @Override
            public void onRemoved(List<String> items, boolean auto) {
                latchRemoved.countDown();
            }

        });

        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        arrayToFile.append("test4");
        arrayToFile.append("test5");
        arrayToFile.append("test6");
        assertTrue(latchAdded.await(1, TimeUnit.SECONDS));

        // 2 items on buffer, 4 on flushed into file
        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(4);
        arrayToFile.flushCache();
        assertTrue(latchFlushed.await(1, TimeUnit.SECONDS));

        // 2 items on buffer, 2 into file, 2 deleted from file
        arrayToFile.setReleaseFileSize(2);
        arrayToFile.setMaxFileSize(4);
        arrayToFile.flushCache();
        assertTrue(latchRemoved.await(1, TimeUnit.SECONDS));
    }

    @Test
    void testCountMethods() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        // no data
        assertEquals(0, arrayToFile.count());
        assertEquals(0, arrayToFile.countBuffered());
        assertEquals(0, arrayToFile.countFile());

        // 3 items in buffer, 0 items into file
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        assertEquals(3, arrayToFile.count());
        assertEquals(3, arrayToFile.countBuffered());
        assertEquals(0, arrayToFile.countFile());

        // 1 item in buffer, 2 items into file
        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(arrayToFile.countBuffered());
        arrayToFile.flushCache();
        assertEquals(3, arrayToFile.count());
        assertEquals(1, arrayToFile.countBuffered());
        assertEquals(2, arrayToFile.countFile());

        // 0 items in buffer, 3 items into file
        arrayToFile.storeCache();
        assertEquals(3, arrayToFile.count());
        assertEquals(0, arrayToFile.countBuffered());
        assertEquals(3, arrayToFile.countFile());

    }

    @Test
    void testGetFirstMethods() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        // no data
        assertNull(arrayToFile.getFirst());
        assertNull(arrayToFile.getFirstBuffered());
        assertNull(arrayToFile.getFirstFile());

        // 3 items in buffer, 0 items into file
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        assertEquals("test1", arrayToFile.getFirst());
        assertEquals("test1", arrayToFile.getFirstBuffered());
        assertNull(arrayToFile.getFirstFile());

        // 1 item in buffer, 2 items into file
        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(arrayToFile.countBuffered());
        arrayToFile.flushCache();
        assertEquals("test1", arrayToFile.getFirst());
        assertEquals("test3", arrayToFile.getFirstBuffered());
        assertEquals("test1", arrayToFile.getFirstFile());

        // 0 items in buffer, 3 items into file
        arrayToFile.storeCache();
        assertEquals("test1", arrayToFile.getFirst());
        assertNull(arrayToFile.getFirstBuffered());
        assertEquals("test1", arrayToFile.getFirstFile());
    }

    @Test
    void testGetLastMethods() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        // no data
        assertNull(arrayToFile.getLast());
        assertNull(arrayToFile.getLastBuffered());
        assertNull(arrayToFile.getLastFile());

        // 3 items in buffer, 0 items into file
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        assertEquals("test3", arrayToFile.getLast());
        assertEquals("test3", arrayToFile.getLastBuffered());
        assertNull(arrayToFile.getLastFile());

        // 1 item in buffer, 2 items into file
        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(arrayToFile.countBuffered());
        arrayToFile.flushCache();
        assertEquals("test3", arrayToFile.getLast());
        assertEquals("test3", arrayToFile.getLastBuffered());
        assertEquals("test2", arrayToFile.getLastFile());

        // 0 items in buffer, 3 items into file
        arrayToFile.storeCache();
        assertEquals("test3", arrayToFile.getLast());
        assertNull(arrayToFile.getLastBuffered());
        assertEquals("test3", arrayToFile.getLastFile());
    }

    @Test
    void testGetMethods() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        // no data
        List<String> all = arrayToFile.getAll();
        assertNotNull(all);
        assertEquals(0, all.size());

        // 3 items in buffer, 0 items into file
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        all = arrayToFile.getAll();
        assertEquals(3, all.size());
        assertTrue(all.contains("test1"));
        assertTrue(all.contains("test2"));
        assertTrue(all.contains("test3"));

        // 1 item in buffer, 2 items into file
        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(arrayToFile.countBuffered());
        arrayToFile.flushCache();
        all = arrayToFile.getAll();
        assertEquals(3, all.size());
        assertTrue(all.contains("test1"));
        assertTrue(all.contains("test2"));
        assertTrue(all.contains("test3"));

        // 0 items in buffer, 3 items into file
        arrayToFile.storeCache();
        all = arrayToFile.getAll();
        assertEquals(3, all.size());
        assertTrue(all.contains("test1"));
        assertTrue(all.contains("test2"));
        assertTrue(all.contains("test3"));
    }


    @Test
    void testGetLatestMethod() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestJavaJSONArrayToFile arrayToFile = new TestJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        // no data
        List<String> latest = arrayToFile.getLatest(2);
        assertNotNull(latest);
        assertEquals(0, latest.size());

        // 3 items in buffer, 0 items into file
        arrayToFile.append("test1");
        arrayToFile.append("test2");
        arrayToFile.append("test3");
        latest = arrayToFile.getLatest(2);
        assertEquals(2, latest.size());
        assertTrue(latest.contains("test2"));
        assertTrue(latest.contains("test3"));

        // 1 item in buffer, 2 items into file
        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(arrayToFile.countBuffered());
        arrayToFile.flushCache();
        latest = arrayToFile.getLatest(2);
        assertEquals(2, latest.size());
        assertTrue(latest.contains("test2"));
        assertTrue(latest.contains("test3"));

        // 0 items in buffer, 3 items into file
        arrayToFile.storeCache();
        latest = arrayToFile.getLatest(2);
        assertEquals(2, latest.size());
        assertTrue(latest.contains("test2"));
        assertTrue(latest.contains("test3"));
    }

    @Test
    void testGetAncientMethod() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestDataJavaJSONArrayToFile arrayToFile = new TestDataJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        // no data
        List<TestData> ancient = arrayToFile.getAncient(2);
        assertNotNull(ancient);
        assertEquals(0, ancient.size());

        // 3 items in buffer, 0 items into file
        arrayToFile.append(new TestData(1, new Date(1000), "test1"));
        arrayToFile.append(new TestData(2, new Date(2000), "test2"));
        arrayToFile.append(new TestData(3, new Date(3000), "test3"));
        ancient = arrayToFile.getAncient(2);
        assertEquals(2, ancient.size());
        assertEquals(1, ancient.get(0).getId());
        assertEquals(2, ancient.get(1).getId());

        // 1 item in buffer, 2 items into file
        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(arrayToFile.countBuffered());
        arrayToFile.flushCache();
        ancient = arrayToFile.getAncient(2);
        assertEquals(2, ancient.size());
        assertEquals(1, ancient.get(0).getId());
        assertEquals(2, ancient.get(1).getId());

        // 0 items in buffer, 3 items into file
        arrayToFile.storeCache();
        ancient = arrayToFile.getAncient(2);
        assertEquals(2, ancient.size());
        assertEquals(1, ancient.get(0).getId());
        assertEquals(2, ancient.get(1).getId());
    }

    @Test
    void testGetByIdMethod() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestDataJavaJSONArrayToFile arrayToFile = new TestDataJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        // no data
        List<TestData> byId = arrayToFile.getById(1L, 2L);
        assertNotNull(byId);
        assertEquals(0, byId.size());

        // 3 items in buffer, 0 items into file
        arrayToFile.append(new TestData(1, new Date(1000), "test1"));
        arrayToFile.append(new TestData(2, new Date(2000), "test2"));
        arrayToFile.append(new TestData(3, new Date(3000), "test3"));
        byId = arrayToFile.getById(1L, 2L);
        assertEquals(2, byId.size());
        assertEquals(1, byId.get(0).getId());
        assertEquals(2, byId.get(1).getId());

        // 1 item in buffer, 2 items into file
        // 2 items from file
        arrayToFile.setReleaseBufferSize(2);
        arrayToFile.setMaxBufferSize(arrayToFile.countBuffered());
        arrayToFile.flushCache();
        byId = arrayToFile.getById(1L, 2L);
        assertEquals(2, byId.size());
        assertEquals(1, byId.get(0).getId());
        assertEquals(2, byId.get(1).getId());

        // 1 item in buffer, 2 items into file
        // 1 item from buffer, 1 item from file
        byId = arrayToFile.getById(2L, 3L);
        assertEquals(2, byId.size());
        assertEquals(2, byId.get(0).getId());
        assertEquals(3, byId.get(1).getId());

        // 0 items in buffer, 3 items into file
        arrayToFile.storeCache();
        byId = arrayToFile.getById(1L, 2L);
        assertEquals(2, byId.size());
        assertEquals(1, byId.get(0).getId());
        assertEquals(2, byId.get(1).getId());
    }

    @Test
    void testGetByDataMethod() throws IOException {
        File jsonFile = File.createTempFile("data_empty", ".json");
        TestDataJavaJSONArrayToFile arrayToFile = new TestDataJavaJSONArrayToFile(jsonFile, KEEP_IN_MEMORY);

        // no data
        Date fromDate = new Date(0);
        Date toDate = new Date(2000);
        List<TestData> byData = arrayToFile.getByData(fromDate, toDate);
        assertNotNull(byData);
        assertEquals(0, byData.size());

        // 3 items in buffer, 0 items into file
        arrayToFile.append(new TestData(1, new Date(1000), "test1"));
        arrayToFile.append(new TestData(2, new Date(2000), "test2"));
        arrayToFile.append(new TestData(3, new Date(3000), "test3"));
        byData = arrayToFile.getByData(fromDate, toDate);
        assertEquals(2, byData.size());
        assertEquals(1, byData.get(0).getId());
        assertEquals(2, byData.get(1).getId());

        // 1 item in buffer, 2 items into file
        // 2 items from file
        byData = arrayToFile.getByData(fromDate, toDate);
        assertEquals(2, byData.size());
        assertEquals(1, byData.get(0).getId());
        assertEquals(2, byData.get(1).getId());

        // 1 item in buffer, 2 items into file
        // 1 item from buffer, 1 item from file
        byData = arrayToFile.getByData(new Date(fromDate.getTime() + 1001), new Date(toDate.getTime() + 1000));
        assertEquals(2, byData.size());
        assertEquals(2, byData.get(0).getId());
        assertEquals(3, byData.get(1).getId());

        // 0 items in buffer, 3 items into file
        arrayToFile.storeCache();
        byData = arrayToFile.getByData(fromDate, toDate);
        assertEquals(2, byData.size());
        assertEquals(1, byData.get(0).getId());
        assertEquals(2, byData.get(1).getId());
    }

}
