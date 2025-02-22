package com.ns.common.dump;

import java.io.File;
import java.lang.management.ManagementFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DumpService {
    private final String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

    public String generateHeapDump() {
        try {
            String dumpFile = "/tmp/heapdump_" + System.currentTimeMillis() + ".hprof";

            ProcessBuilder pb = new ProcessBuilder("jcmd", pid, "GC.heap_dump", dumpFile);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return dumpFile;
            } else {
                log.error("generateHeapDump error - exit code: " + exitCode);
                return null;
            }

        } catch (Exception e) {
            log.error("generateHeapDump error - " + e);
            return null;
        }
    }

    public String generateThreadDump() {
        try {
            String dumpFile = "/tmp/threaddump_" + System.currentTimeMillis() + ".txt";

            ProcessBuilder pb = new ProcessBuilder("jcmd", pid, "Thread.print");
            pb.redirectOutput(new File(dumpFile));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return dumpFile;
            } else {
                log.error("generateThreadDump error - exit code: " + exitCode);
                return null;
            }

        } catch (Exception e) {
            log.error("generateThreadDump error - " + e);
            return null;
        }
    }
}
