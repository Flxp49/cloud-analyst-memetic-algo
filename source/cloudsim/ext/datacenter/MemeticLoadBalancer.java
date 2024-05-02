package cloudsim.ext.datacenter;

import java.util.Map;

import cloudsim.ext.Constants;
import cloudsim.ext.event.CloudSimEvent;
import cloudsim.ext.event.CloudSimEventListener;
import cloudsim.ext.event.CloudSimEvents;

public class MemeticLoadBalancer extends VmLoadBalancer implements CloudSimEventListener {
    private Map<Integer, VirtualMachineState> vmStatesList;
    private Map<Integer, Integer> currentAllocationCounts;
    public MemeticLoadBalancer(DatacenterController dcb) {
        //setup here
        dcb.addCloudSimEventListener(this);
        this.vmStatesList = dcb.getVmStatesList();
    }
    // this function is what allocates the next VM
    public int getNextAvailableVm() {
        int vmId = -1;
        //If all available vms are not allocated, allocate the new ones
		if (currentAllocationCounts.size() < vmStatesList.size()){
			for (int availableVmId : vmStatesList.keySet()){
				if (!currentAllocationCounts.containsKey(availableVmId)){
					vmId = availableVmId;
					break; 
				}				
			}
        } else {
            // ALGOOOOO
            // JOb J -> VM
            // vm1 - 2 memory 1 cpu = Value X
            // vm2 - 2 memory 1 cpu = Value Y
            // vm3 - 2 memory 1 cpu = Value Z
            //

        }
        allocatedVm(vmId);
        return vmId;
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
			
		} else if (e.getId() == CloudSimEvents.EVENT_VM_FINISHED_CLOUDLET){
			int vmId = (Integer) e.getParameter(Constants.PARAM_VM_ID);
			Integer currCount = currentAllocationCounts.remove(vmId);
			if (currCount != null){
				currCount--;
				currentAllocationCounts.put(vmId, currCount);
			}
		}
	}
}
