package com.bhargrah.durablecachems.wal;


import com.bhargrah.durablecachems.wal.command.Command;
import com.bhargrah.durablecachems.wal.command.SetValueCommand;
import com.bhargrah.durablecachems.wal.common.Config;
import com.bhargrah.durablecachems.wal.entity.WALEntry;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DurableCacheStore {
    //persistent..
    private final Map<String, String> kv = new HashMap<>();

    public String get(String key) {
        return kv.get(key);
    }

    public void put(String key, String value) {
        //Then applyLog at startup.
        //e.g. Cassandra's WAL for mem-table.
        //pipeline
        //async
        //queue of requests |put() | put| put | | |-->
        // <--|resonse | put| put | | |
        //async arrayblockqueue
        appendLog(key, value);
        //async but preserver order
        kv.put(key, value);
        //async
        //respond to client
    }

    private Long appendLog(String key, String value) {
        Long aLong = wal.writeEntry(new SetValueCommand(key, value).serialize());
        wal.flush(); //heavy operation.
        return aLong;
    }

    //@VisibleForTesting
    final WriteAheadLog wal;
    private final Config config;

    public DurableCacheStore(Config config) {
        this.config = config;
        this.wal = WriteAheadLog.openWAL(config);
        applyLog();
    }

    public void applyLog() {
        List<WALEntry> walEntries = wal.readAll();
        applyEntries(walEntries);
    }

    private void applyEntries(List<WALEntry> walEntries) {
        for (WALEntry walEntry : walEntries) {
            Command command = deserialize(walEntry);
            if (command instanceof SetValueCommand) {
                SetValueCommand setValueCommand = (SetValueCommand) command;
                kv.put(setValueCommand.getKey(), setValueCommand.getValue());
            }
        }
    }

    private Command deserialize(WALEntry walEntry) {
        return Command.deserialize(new ByteArrayInputStream(walEntry.getData()));
    }

    public void close() {
        wal.close();
        kv.clear();
    }

    public Collection<String> values() {
        return kv.values();
    }
}
