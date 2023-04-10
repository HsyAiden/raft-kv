package net.items.server.log;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @ Author Hsy
 * @ Date 2023/04/02
 * @ describe
 **/
@Slf4j
public class LogModule {


    private String dbDir;

    private String logsDir;

    private RocksDB logDb;

    /**
     * 记录 logModule 中最后一个日志索引
     */
    private static final byte[] LAST_INDEX_KEY = "LAST_INDEX_KEY".getBytes();

    /**
     * 写锁
     */
    private ReentrantLock lock = new ReentrantLock();

    private LogModule() {

        if (dbDir == null) {
            dbDir = "./RocksDB-raft" + System.getProperty("server.prot");
        }
        if (logsDir == null) {
            logsDir = dbDir + "/logModule";
        }
        RocksDB.loadLibrary();
        Options options = new Options();
        options.setCreateIfMissing(true);

        File file = new File(logsDir);
        boolean success = false;
        if (!file.exists()) {
            // 创建日志目录
            success = file.mkdirs();
        }
        if (success) {
            log.info("make a new dir : " + logsDir);
        }
        try {
            // 使用RocksDB打开日志文件
            logDb = RocksDB.open(options, logsDir);
        } catch (RocksDBException e) {
            log.warn(e.getMessage());
        }
    }

    public static LogModule getInstance() {
        return LogModuleLazyHolder.INSTANCE;
    }

    private static class LogModuleLazyHolder {

        private static final LogModule INSTANCE = new LogModule();
    }

    public void write(LogEntry logEntry) {
        boolean success = false;
        try {
            lock.lock();
            logEntry.setIndex(getLastIndex() + 1);
            logDb.put(logEntry.getIndex().toString().getBytes(), JSON.toJSONBytes(logEntry));
            success = true;
            log.info("DefaultLogModule write rocksDB success, logEntry info : [{}]", logEntry);
        } catch (RocksDBException e) {
            log.warn(e.getMessage());
        } finally {
            if (success) {
                updateLastIndex(logEntry.getIndex());
            }
            lock.unlock();
        }
    }

    /**
     * 根据索引读取日志
     * @param index
     * @return 日志条目 LogEntry 或 null
     */
    public LogEntry read(Long index) {
        try {
            byte[] result = logDb.get(convert(index));
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (RocksDBException e) {
            log.warn(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 删除index在[startIndex, lastIndex]内的日志
     * @param startIndex
     */
    public void removeOnStartIndex(Long startIndex) {
        boolean success = false;
        int count = 0;
        try {
            //lock.tryLock(3000, MILLISECONDS);
            lock.lock();
            for (long i = startIndex; i <= getLastIndex(); i++) {
                logDb.delete(String.valueOf(i).getBytes());
                ++count;
            }
            success = true;
            log.warn("rocksDB removeOnStartIndex success, count={} startIndex={}, lastIndex={}", count, startIndex, getLastIndex());
        } catch (RocksDBException e) {
            log.warn(e.getMessage());
        } finally {
            if (success) {
                updateLastIndex(getLastIndex() - count);
            }
            lock.unlock();
        }
    }

    public LogEntry getLast() {
        try {
            byte[] result = logDb.get(convert(getLastIndex()));
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (RocksDBException e) {
            log.error("RocksDB getLast error", e);
        }
        return null;
    }

    /**
     * 获取最后一个日志的index，没有日志时返回-1
     * @return
     */
    public Long getLastIndex() {
        byte[] lastIndex = "-1".getBytes();
        try {
            lastIndex = logDb.get(LAST_INDEX_KEY);
            if (lastIndex == null) {
                lastIndex = "-1".getBytes();
            }
        } catch (RocksDBException e) {
            log.error("RocksDB getLastIndex error", e);
        }
        return Long.valueOf(new String(lastIndex));
    }

    // on lock
    private void updateLastIndex(Long index) {
        try {
            // overWrite
            logDb.put(LAST_INDEX_KEY, index.toString().getBytes());
        } catch (RocksDBException e) {
            log.error("RocksDB updateLastIndex error", e);
        }
    }

    private byte[] convert(Long key) {
        return key.toString().getBytes();
    }


}
