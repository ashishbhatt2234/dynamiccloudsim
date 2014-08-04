package de.huberlin.wbi.dcs.examples;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.distributions.ContinuousDistribution;

import de.huberlin.wbi.dcs.CloudletSchedulerGreedyDivided;
import de.huberlin.wbi.dcs.DynamicHost;
import de.huberlin.wbi.dcs.DynamicModel;
import de.huberlin.wbi.dcs.DynamicVm;
import de.huberlin.wbi.dcs.VmAllocationPolicyRandom;
import de.huberlin.wbi.dcs.workflow.Task;
import de.huberlin.wbi.dcs.workflow.Workflow;
import de.huberlin.wbi.dcs.workflow.io.AlignmentTraceFileReader;
import de.huberlin.wbi.dcs.workflow.io.CuneiformLogFileReader;
import de.huberlin.wbi.dcs.workflow.io.DaxFileReader;
import de.huberlin.wbi.dcs.workflow.io.MontageTraceFileReader;
import de.huberlin.wbi.dcs.workflow.scheduler.C2O;
import de.huberlin.wbi.dcs.workflow.scheduler.C3;
import de.huberlin.wbi.dcs.workflow.scheduler.GreedyQueueScheduler;
import de.huberlin.wbi.dcs.workflow.scheduler.HEFTScheduler;
import de.huberlin.wbi.dcs.workflow.scheduler.LATEScheduler;
import de.huberlin.wbi.dcs.workflow.scheduler.StaticRoundRobinScheduler;
import de.huberlin.wbi.dcs.workflow.scheduler.WorkflowScheduler;

public class WorkflowExample {

	// datacenter params
	protected long bwpsPerPe = 256;
	protected long iopsPerPe = 20 * 1024;

	protected int nOpteron270 = 200;
	protected int nCusPerCoreOpteron270 = 2;
	protected int nCoresOpteron270 = 4;
	protected int mipsPerCoreOpteron270 = 174;

	protected int nOpteron2218 = 200;
	protected int nCusPerCoreOpteron2218 = 2;
	protected int nCoresOpteron2218 = 4;
	protected int mipsPerCoreOpteron2218 = 247;

	protected int nXeonE5430 = 100;
	protected int nCusPerCoreXeonE5430 = 2;
	protected int nCoresXeonE5430 = 8;
	protected int mipsPerCoreXeonE5430 = 355;

	// vm params
	protected int nVms = 8;
	protected int taskSlotsPerVm = 1;

	protected double numberOfCusPerPe = 1;
	protected int numberOfPes = 1;
	protected int ram = (int) (1.7 * 1024);

	public static void main(String[] args) {
		double totalRuntime = 0d;
		Parameters.parseParameters(args);

		try {
			for (int i = 0; i < Parameters.numberOfRuns; i++) {
				WorkflowExample ex = new WorkflowExample();
				if (!Parameters.outputDatacenterEvents) {
					Log.disable();
				}
				// Initialize the CloudSim package
				int num_user = 1; // number of grid users
				Calendar calendar = Calendar.getInstance();
				boolean trace_flag = false; // mean trace events
				CloudSim.init(num_user, calendar, trace_flag);

				ex.createDatacenter("Datacenter");
				WorkflowScheduler scheduler = ex.createScheduler();
				ex.createVms(i, scheduler);
				Workflow workflow = buildWorkflow(scheduler);
				ex.submitWorkflow(workflow, scheduler);

				// Start the simulation
				CloudSim.startSimulation();
				CloudSim.stopSimulation();

				totalRuntime += scheduler.getRuntime();
				System.out.println(scheduler.getRuntime() / 60);
			}

			Log.printLine("Average runtime in minutes: " + totalRuntime
					/ Parameters.numberOfRuns / 60);
			Log.printLine("Total Workload: " + Task.getTotalMi() + "mi "
					+ Task.getTotalIo() + "io " + Task.getTotalBw() + "bw");
			Log.printLine("Total VM Performance: " + DynamicHost.getTotalMi()
					+ "mips " + DynamicHost.getTotalIo() + "iops "
					+ DynamicHost.getTotalBw() + "bwps");
			Log.printLine("minimum minutes (quotient): " + Task.getTotalMi()
					/ DynamicHost.getTotalMi() / 60 + " " + Task.getTotalIo()
					/ DynamicHost.getTotalIo() / 60 + " " + Task.getTotalBw()
					/ DynamicHost.getTotalBw() / 60);
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}

	}

	public WorkflowScheduler createScheduler() {
		try {
			switch (Parameters.scheduler) {
			case STATIC_ROUND_ROBIN:
				return new StaticRoundRobinScheduler(
						"StaticRoundRobinScheduler", taskSlotsPerVm);
			case LATE:
				return new LATEScheduler("LATEScheduler", taskSlotsPerVm);
			case HEFT:
				return new HEFTScheduler("HEFTScheduler", taskSlotsPerVm);
			case JOB_QUEUE:
				return new GreedyQueueScheduler("GreedyQueueScheduler",
						taskSlotsPerVm);
			case C3:
				return new C3("C3", taskSlotsPerVm);
			case C2O:
				return new C2O("C2O", taskSlotsPerVm);
			default:
				return new GreedyQueueScheduler("GreedyQueueScheduler",
						taskSlotsPerVm);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void createVms(int run, WorkflowScheduler scheduler) {
		// Create VMs
		List<Vm> vmlist = createVMList(scheduler.getId(), run);
		scheduler.submitVmList(vmlist);
	}

	public static Workflow buildWorkflow(WorkflowScheduler scheduler) {
		switch (Parameters.experiment) {
		case MONTAGE_TRACE_1:
			return new MontageTraceFileReader().parseLogFile(scheduler.getId(),
					"examples/montage.m17.1.trace", true, true, ".*jpg");
		case MONTAGE_TRACE_12:
			return new MontageTraceFileReader().parseLogFile(scheduler.getId(),
					"examples/montage.m17.12.trace", true, true, ".*jpg");
		case ALIGNMENT_TRACE:
			return new AlignmentTraceFileReader().parseLogFile(
					scheduler.getId(), "examples/alignment.caco.geo.chr22.trace2", true,
					true, null);
		case MONTAGE_25:
			return new DaxFileReader().parseLogFile(scheduler.getId(),
					"examples/Montage_25.xml", true, true, null);
		case MONTAGE_1000:
			return new DaxFileReader().parseLogFile(scheduler.getId(),
					"examples/Montage_1000.xml", true, true, null);
		case CYBERSHAKE_1000:
			return new DaxFileReader().parseLogFile(scheduler.getId(),
					"examples/CyberShake_1000.xml", true, true, null);
		case EPIGENOMICS_997:
			return new DaxFileReader().parseLogFile(scheduler.getId(),
					"examples/Epigenomics_997.xml", true, true, null);
		case CUNEIFORM_VARIANT_CALL:
			return new CuneiformLogFileReader().parseLogFile(scheduler.getId(),
					"examples/i1_s11756_r7_greedyQueue.log", true, true, null);
		}
		return null;
	}

	public void submitWorkflow(Workflow workflow, WorkflowScheduler scheduler) {
		// Create Cloudlets and send them to Scheduler
		if (Parameters.outputWorkflowGraph) {
			workflow.visualize(1920, 1200);
		}
		scheduler.submitWorkflow(workflow);
	}

	// all numbers in 1000 (e.g. kb/s)
	public Datacenter createDatacenter(String name) {
		Random numGen;
		numGen = Parameters.numGen;
		List<DynamicHost> hostList = new ArrayList<DynamicHost>();
		int hostId = 0;
		long storage = 1024 * 1024;

		int ram = (int) (2 * 1024 * nCusPerCoreOpteron270 * nCoresOpteron270);
		for (int i = 0; i < nOpteron270; i++) {
			double mean = 1d;
			double dev = Parameters.bwHeterogeneityCV;
			ContinuousDistribution dist = Parameters.getDistribution(
					Parameters.bwHeterogeneityDistribution, mean,
					Parameters.bwHeterogeneityAlpha,
					Parameters.bwHeterogeneityBeta, dev,
					Parameters.bwHeterogeneityShape,
					Parameters.bwHeterogeneityLocation,
					Parameters.bwHeterogeneityShift,
					Parameters.bwHeterogeneityMin,
					Parameters.bwHeterogeneityMax,
					Parameters.bwHeterogeneityPopulation);
			long bwps = 0;
			while (bwps <= 0) {
				bwps = (long) (dist.sample() * bwpsPerPe);
			}
			mean = 1d;
			dev = Parameters.ioHeterogeneityCV;
			dist = Parameters.getDistribution(
					Parameters.ioHeterogeneityDistribution, mean,
					Parameters.ioHeterogeneityAlpha,
					Parameters.ioHeterogeneityBeta, dev,
					Parameters.ioHeterogeneityShape,
					Parameters.ioHeterogeneityLocation,
					Parameters.ioHeterogeneityShift,
					Parameters.ioHeterogeneityMin,
					Parameters.ioHeterogeneityMax,
					Parameters.ioHeterogeneityPopulation);
			long iops = 0;
			while (iops <= 0) {
				iops = (long) (long) (dist.sample() * iopsPerPe);
			}
			mean = 1d;
			dev = Parameters.cpuHeterogeneityCV;
			dist = Parameters.getDistribution(
					Parameters.cpuHeterogeneityDistribution, mean,
					Parameters.cpuHeterogeneityAlpha,
					Parameters.cpuHeterogeneityBeta, dev,
					Parameters.cpuHeterogeneityShape,
					Parameters.cpuHeterogeneityLocation,
					Parameters.cpuHeterogeneityShift,
					Parameters.cpuHeterogeneityMin,
					Parameters.cpuHeterogeneityMax,
					Parameters.cpuHeterogeneityPopulation);
			long mips = 0;
			while (mips <= 0) {
				mips = (long) (long) (dist.sample() * mipsPerCoreOpteron270);
			}
			if (numGen.nextDouble() < Parameters.likelihoodOfStraggler) {
				bwps *= Parameters.stragglerPerformanceCoefficient;
				iops *= Parameters.stragglerPerformanceCoefficient;
				mips *= Parameters.stragglerPerformanceCoefficient;
			}
			hostList.add(new DynamicHost(hostId++, ram, bwps, iops, storage,
					nCusPerCoreOpteron270, nCoresOpteron270, mips));
		}

		ram = (int) (2 * 1024 * nCusPerCoreOpteron2218 * nCoresOpteron2218);
		for (int i = 0; i < nOpteron2218; i++) {
			double mean = 1d;
			double dev = Parameters.bwHeterogeneityCV;
			ContinuousDistribution dist = Parameters.getDistribution(
					Parameters.bwHeterogeneityDistribution, mean,
					Parameters.bwHeterogeneityAlpha,
					Parameters.bwHeterogeneityBeta, dev,
					Parameters.bwHeterogeneityShape,
					Parameters.bwHeterogeneityLocation,
					Parameters.bwHeterogeneityShift,
					Parameters.bwHeterogeneityMin,
					Parameters.bwHeterogeneityMax,
					Parameters.bwHeterogeneityPopulation);
			long bwps = 0;
			while (bwps <= 0) {
				bwps = (long) (dist.sample() * bwpsPerPe);
			}
			mean = 1d;
			dev = Parameters.ioHeterogeneityCV;
			dist = Parameters.getDistribution(
					Parameters.ioHeterogeneityDistribution, mean,
					Parameters.ioHeterogeneityAlpha,
					Parameters.ioHeterogeneityBeta, dev,
					Parameters.ioHeterogeneityShape,
					Parameters.ioHeterogeneityLocation,
					Parameters.ioHeterogeneityShift,
					Parameters.ioHeterogeneityMin,
					Parameters.ioHeterogeneityMax,
					Parameters.ioHeterogeneityPopulation);
			long iops = 0;
			while (iops <= 0) {
				iops = (long) (long) (dist.sample() * iopsPerPe);
			}
			mean = 1d;
			dev = Parameters.cpuHeterogeneityCV;
			dist = Parameters.getDistribution(
					Parameters.cpuHeterogeneityDistribution, mean,
					Parameters.cpuHeterogeneityAlpha,
					Parameters.cpuHeterogeneityBeta, dev,
					Parameters.cpuHeterogeneityShape,
					Parameters.cpuHeterogeneityLocation,
					Parameters.cpuHeterogeneityShift,
					Parameters.cpuHeterogeneityMin,
					Parameters.cpuHeterogeneityMax,
					Parameters.cpuHeterogeneityPopulation);
			long mips = 0;
			while (mips <= 0) {
				mips = (long) (long) (dist.sample() * mipsPerCoreOpteron2218);
			}
			if (numGen.nextDouble() < Parameters.likelihoodOfStraggler) {
				bwps *= Parameters.stragglerPerformanceCoefficient;
				iops *= Parameters.stragglerPerformanceCoefficient;
				mips *= Parameters.stragglerPerformanceCoefficient;
			}
			hostList.add(new DynamicHost(hostId++, ram, bwps, iops, storage,
					nCusPerCoreOpteron2218, nCoresOpteron2218, mips));
		}

		ram = (int) (2 * 1024 * nCusPerCoreXeonE5430 * nCoresXeonE5430);
		for (int i = 0; i < nXeonE5430; i++) {
			double mean = 1d;
			double dev = Parameters.bwHeterogeneityCV;
			ContinuousDistribution dist = Parameters.getDistribution(
					Parameters.bwHeterogeneityDistribution, mean,
					Parameters.bwHeterogeneityAlpha,
					Parameters.bwHeterogeneityBeta, dev,
					Parameters.bwHeterogeneityShape,
					Parameters.bwHeterogeneityLocation,
					Parameters.bwHeterogeneityShift,
					Parameters.bwHeterogeneityMin,
					Parameters.bwHeterogeneityMax,
					Parameters.bwHeterogeneityPopulation);
			long bwps = 0;
			while (bwps <= 0) {
				bwps = (long) (dist.sample() * bwpsPerPe);
			}
			mean = 1d;
			dev = Parameters.ioHeterogeneityCV;
			dist = Parameters.getDistribution(
					Parameters.ioHeterogeneityDistribution, mean,
					Parameters.ioHeterogeneityAlpha,
					Parameters.ioHeterogeneityBeta, dev,
					Parameters.ioHeterogeneityShape,
					Parameters.ioHeterogeneityLocation,
					Parameters.ioHeterogeneityShift,
					Parameters.ioHeterogeneityMin,
					Parameters.ioHeterogeneityMax,
					Parameters.ioHeterogeneityPopulation);
			long iops = 0;
			while (iops <= 0) {
				iops = (long) (long) (dist.sample() * iopsPerPe);
			}
			mean = 1d;
			dev = Parameters.cpuHeterogeneityCV;
			dist = Parameters.getDistribution(
					Parameters.cpuHeterogeneityDistribution, mean,
					Parameters.cpuHeterogeneityAlpha,
					Parameters.cpuHeterogeneityBeta, dev,
					Parameters.cpuHeterogeneityShape,
					Parameters.cpuHeterogeneityLocation,
					Parameters.cpuHeterogeneityShift,
					Parameters.cpuHeterogeneityMin,
					Parameters.cpuHeterogeneityMax,
					Parameters.cpuHeterogeneityPopulation);
			long mips = 0;
			while (mips <= 0) {
				mips = (long) (long) (dist.sample() * mipsPerCoreXeonE5430);
			}
			if (numGen.nextDouble() < Parameters.likelihoodOfStraggler) {
				bwps *= Parameters.stragglerPerformanceCoefficient;
				iops *= Parameters.stragglerPerformanceCoefficient;
				mips *= Parameters.stragglerPerformanceCoefficient;
			}
			hostList.add(new DynamicHost(hostId++, ram, bwps, iops, storage,
					nCusPerCoreXeonE5430, nCoresXeonE5430, mips));
		}

		String arch = "x86";
		String os = "Linux";
		String vmm = "Xen";
		double time_zone = 10.0;
		double cost = 3.0;
		double costPerMem = 0.05;
		double costPerStorage = 0.001;
		double costPerBw = 0.0;
		LinkedList<Storage> storageList = new LinkedList<Storage>();

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics,
					new VmAllocationPolicyRandom(hostList, Parameters.seed++),
					storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	public List<Vm> createVMList(int userId, int run) {

		// Creates a container to store VMs. This list is passed to the broker
		// later
		LinkedList<Vm> list = new LinkedList<Vm>();

		// VM Parameters
		long storage = 10000;
		String vmm = "Xen";

		// create VMs
		Vm[] vm = new DynamicVm[nVms];

		for (int i = 0; i < nVms; i++) {
			DynamicModel dynamicModel = new DynamicModel();
			vm[i] = new DynamicVm(i, userId, numberOfCusPerPe, numberOfPes,
					ram, storage, vmm, new CloudletSchedulerGreedyDivided(),
					dynamicModel, "output/run_" + run + "_vm_" + i + ".csv",
					taskSlotsPerVm);
			list.add(vm[i]);
		}

		return list;
	}

}
