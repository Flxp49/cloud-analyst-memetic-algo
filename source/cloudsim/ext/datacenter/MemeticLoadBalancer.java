package cloudsim.ext.datacenter;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;

// import cloudsim.VirtualMachine;
import cloudsim.ext.Constants;
import cloudsim.ext.event.CloudSimEvent;
import cloudsim.ext.event.CloudSimEventListener;
import cloudsim.ext.event.CloudSimEvents;
// import cloudsim.ext.InternetCloudlet;

public class MemeticLoadBalancer extends VmLoadBalancer implements CloudSimEventListener {
	/** Holds the VMs along with their states */
    private Map<Integer, VirtualMachineState> vmStatesList;
	/** Holds the count current active allcoations on each VM */
    private Map<Integer, Integer> currentAllocationCounts;
	/** Holds the mapping of each vm with job  */
    private Map<Integer, Integer> jobMappings;
    // private DatacenterController localDCB;
    public MemeticLoadBalancer(DatacenterController dcb) {
        //setup here
		// this.localDCB = dcb;
        dcb.addCloudSimEventListener(this);
        this.vmStatesList = dcb.getVmStatesList();
		currentAllocationCounts = new ConcurrentHashMap<Integer, Integer>();
		jobMappings = new ConcurrentHashMap<Integer, Integer>();
    }
    // this function is what allocates the next VM
    // public int getNextAvailableVm(InternetCloudlet cl) {
    public int getNextAvailableVm() {
        int vmId = -1;
        //If all available vms are not allocated, allocated the new ones
		if (currentAllocationCounts.size() < vmStatesList.size()){
			for (int availableVmId : vmStatesList.keySet()){
				if (!currentAllocationCounts.containsKey(availableVmId)){
					vmId = availableVmId;
					break;
				}				
			}
		} 
		else {
			for (int thisVmId : currentAllocationCounts.keySet()){
				if (currentAllocationCounts.get(thisVmId)==0) {
					vmId = thisVmId;
					break;
				}
			}
		} if (vmId == -1) {
			// System.out.println("ALLOCATIONS: "+currentAllocationCounts);
			for (int thisVmId : currentAllocationCounts.keySet()){
				jobMappings.put(thisVmId, currentAllocationCounts.get(thisVmId));
				System.out.println(currentAllocationCounts.get(thisVmId));
			}
			double fitness = fitnessCost();
			for (int id : jobMappings.keySet()){
				if (jobMappings.get(id) > fitness) {
					jobMappings.remove(id);
				}
			}

			int totalAllocations = 0;
			for (int id:jobMappings.keySet()) {
				totalAllocations+=jobMappings.get(id);
			}
			Map<Integer, Double> probabilitiesMap = new HashMap<>();
			for (int id:jobMappings.keySet()) {
				probabilitiesMap.put(id, (double)jobMappings.get(id)/totalAllocations); 
			}
			double[] cumulativeProb = new double[probabilitiesMap.size()];
			int index=0;
			for (int id:probabilitiesMap.keySet()) {
				if (index == 0) {
					cumulativeProb[index] = probabilitiesMap.get(id);
				} else {
					cumulativeProb[index] = probabilitiesMap.get(id)+cumulativeProb[index-1];
				}
				index++;
			}
			Arrays.sort(cumulativeProb);

			Random random = new Random();
        	double randomNumber = random.nextDouble(1);
			// System.out.println("RANDOM:"+randomNumber);
			int foundIndex =0;
			// System.out.println("DEBUG");
			// for(int i = 0; i<cumulativeProb.length; i++)
			// {
			// 	System.out.println(cumulativeProb[i]);
			// }
			for (int i=0; i<cumulativeProb.length; i++) {
				if (randomNumber <= cumulativeProb[i]) {
					foundIndex = i;
					break;
				}
			}
			for (int Vmid : probabilitiesMap.keySet()) {
				if (probabilitiesMap.get(Vmid) == cumulativeProb[foundIndex]) {
					vmId = Vmid;
				}
			}
		}
        allocatedVm(vmId);
        return vmId;
    }

	public double fitnessCost() {
		int totalAllocations=0;
		double avg=0;
		for (int allocations:jobMappings.values()){
			totalAllocations+=allocations;
		}	
		avg =  totalAllocations/jobMappings.size();
		return avg;
	}

    public void cloudSimEventFired(CloudSimEvent e) {
		if (e.getId() == CloudSimEvents.EVENT_CLOUDLET_ALLOCATED_TO_VM){
			int vmId = (Integer) e.getParameter(Constants.PARAM_VM_ID);
			
			Integer currCount = currentAllocationCounts.remove(vmId);
			if (currCount == null){
				currCount = 1;
			} else {
				currCount++;
			}
			currentAllocationCounts.put(vmId, currCount);
		} 
		else if (e.getId() == CloudSimEvents.EVENT_VM_FINISHED_CLOUDLET){
			int vmId = (Integer) e.getParameter(Constants.PARAM_VM_ID);
			Integer currCount = currentAllocationCounts.remove(vmId);
			if (currCount != null){
				currCount--;
				currentAllocationCounts.put(vmId, currCount);
			}
		}
		else if (e.getId() == CloudSimEvents.EVENT_CLOUDLET_ALLOCATED_TO_VM){
			int vmId = (Integer) e.getParameter(Constants.PARAM_VM_ID);
			vmStatesList.put(vmId, VirtualMachineState.BUSY);
		} 
		else if (e.getId() == CloudSimEvents.EVENT_VM_FINISHED_CLOUDLET){
			int vmId = (Integer) e.getParameter(Constants.PARAM_VM_ID);
			vmStatesList.put(vmId, VirtualMachineState.AVAILABLE);
		}
	}
}
