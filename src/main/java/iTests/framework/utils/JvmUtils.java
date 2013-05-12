package iTests.framework.utils;

import org.openspaces.admin.vm.VirtualMachine;

public class JvmUtils {
	
	public static double runGC(VirtualMachine vm) {
        double usedMem1 = getUsedMemory(vm), usedMem2 = Long.MAX_VALUE;
        for (int i = 0; (usedMem1 < usedMem2) && (i < 1000); ++i) {
            vm.runGc();
            usedMem2 = usedMem1;
            usedMem1 = getUsedMemory(vm);
        }
        return getUsedMemory(vm);
    }

    public static double getUsedMemory(VirtualMachine vm) {
        double totalMemory = vm.getDetails().getMemoryHeapMaxInMB();
        double freeMemory = vm.getStatistics().getMemoryHeapUsedInMB();
        double usedMemory = totalMemory - freeMemory;
        return usedMemory;
    }
}
