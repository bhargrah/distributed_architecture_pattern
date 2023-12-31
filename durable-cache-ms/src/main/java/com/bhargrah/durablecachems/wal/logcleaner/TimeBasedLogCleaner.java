package com.bhargrah.durablecachems.wal.logcleaner;


import com.bhargrah.durablecachems.wal.entity.WALSegment;
import com.bhargrah.durablecachems.wal.WriteAheadLog;
import com.bhargrah.durablecachems.wal.common.Config;

import java.util.ArrayList;
import java.util.List;

public class TimeBasedLogCleaner extends LogCleaner {

    public TimeBasedLogCleaner(Config config, WriteAheadLog wal) {
        super(config, wal);
    }

    @Override
    List<WALSegment> getSegmentsToBeDeleted() {
        return getSegmentsPast(config.getLogMaxDurationMs());
    }

    //<codeFragment name="timeBasedLogCleaning"
    private List<WALSegment> getSegmentsPast(Long logMaxDurationMs) {
        long now = System.currentTimeMillis();
        List<WALSegment> markedForDeletion = new ArrayList<>();
        List<WALSegment> sortedSavedSegments = wal.sortedSavedSegments;
        for (WALSegment sortedSavedSegment : sortedSavedSegments) {
            if (timeElaspedSince(now, sortedSavedSegment.getLastLogEntryTimestamp()) > logMaxDurationMs) {
                markedForDeletion.add(sortedSavedSegment);
            }
        }
        return markedForDeletion;
    }

    private long timeElaspedSince(long now, long lastLogEntryTimestamp) {
        return now - lastLogEntryTimestamp;
    }
    //</codeFragment>
}
