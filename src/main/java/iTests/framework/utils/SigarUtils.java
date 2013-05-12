/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package iTests.framework.utils;

import com.gigaspaces.internal.sigar.SigarHolder;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.internal.gsa.DefaultGridServiceAgent;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author adaml
 * 
 */
public class SigarUtils {

	/**
	 * Returns all child processes of a given process pid.
	 * 
	 * @param ppid
	 * @return a Set<String> of process ids.
	 */
	public static Set<String> getChildProcesses(final long ppid) {
		final Sigar sigar = SigarHolder.getSigar();
		final Set<String> children = new HashSet<String>();
		long[] pids;
		try {
			pids = sigar.getProcList();
		} catch (final SigarException se) {
			LogUtils.log("WARNING Failed to get child processes for process " + ppid);
			return children;
		}
		for (final long pid : pids) {
			try {
				if (ppid == sigar.getProcState(pid).getPpid()) {
					children.add(Long.toString(pid));
				}
			} catch (final SigarException e) {
				LogUtils.log("While scanning for child processes of process " + ppid
                        + ", could not read process state of Process: " + pid + ". Ignoring.", e);
			}

		}
		return children;
	}

	/**
	 * 
	 * Returns all child processes of a GS Agent.
	 * 
	 * @param admin
	 * @return a Set<String> of process ids.
	 * @throws java.rmi.RemoteException
	 */
	public static Set<String> getAgentChildProcesses(Admin admin)
			throws RemoteException {
		long agentPid = getAgentPid(admin);
		return getChildProcesses(agentPid);
	}

	private static long getAgentPid(Admin admin)
			throws RemoteException {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne(10, TimeUnit.SECONDS);
		return ((DefaultGridServiceAgent) gsa).getJVMDetails().getPid();
	}

	public static String getProcessName(String pid) {
		final Sigar sigar = SigarHolder.getSigar();
		ProcState procState;
		try {
			procState = sigar.getProcState(pid);
			String processName = procState.getName();
			return processName;
		} catch (SigarException e) {
			LogUtils.log("Process Name could not be obtained.", e);
		}
		return "";
	}

	public static void killProcess(long pid)
			throws SigarException {
		try {
			if (ScriptUtils.isLinuxMachine()) {
				SigarHolder.getSigar().kill(pid, "SIGKILL");
			} else {
				SigarHolder.getSigar().kill(pid, "SIGTERM");
			}
		} catch (SigarException e) {
			throw new IllegalStateException("Failed to kill PID: " + pid, e);
		}

		for (int i = 0; i < 3; ++i) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// ignore
			}

			if (!isProcessAlive(pid)) {
				return;
			}
		}
		
		throw new IllegalStateException("Failed to kill PID: " + pid);
	}

	/*********
	 * Checks, using Sigar, is a given process is alive.
	 * 
	 * @param pid the process pid.
	 * @return true if the process is alive (i.e. not stopped or zombie).
	 */
	public static boolean isProcessAlive(final long pid)
			throws SigarException {

		final ProcState procState = getProcState(pid);
		if (procState == null || procState.getState() == ProcState.STOP || procState.getState() == ProcState.ZOMBIE) {
			return false;
		}

		return true;
	}

	public static ProcState getProcState(final long pid)
			throws SigarException {
		final Sigar sigar = SigarHolder.getSigar();

		ProcState procState = null;
		try {
			procState = sigar.getProcState(pid);
		} catch (final SigarException e) {
			if ("No such process".equals(e.getMessage())) {
				return null;
			}

		}
		return procState;
	}

}
