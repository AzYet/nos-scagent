package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology;

import jp.co.nttdata.ofc.common.core.ScheduledThread;

public class ThreadGenerator implements Runnable{
	private ScheduledThread lldpSenderThread;
	private ScheduledThread lldpCheckerThread;
	private ScheduledThread tableUpdaterThread;

	@Override
	public void run() {
		this.lldpSenderThread = new ScheduledThread(new LldpSender(), TopologyManager.LLDP_INTERVAL);
		this.lldpCheckerThread = new ScheduledThread(new LldpChecker(), TopologyManager.LLDP_INTERVAL);
		this.tableUpdaterThread = new ScheduledThread(new TableUpdater(), TopologyManager.TABLE_UPDATE_INTERVAL);

		try {
			Thread.sleep(TopologyManager.INIT_SLEEP);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if(TopologyManager.USE_LLDP){
			this.startLldpSenderThread();
			this.startLldpCheckerThread();
		}
		this.startTableUpdaterThread();
	}

	private void startLldpSenderThread(){
		System.out.println("LldpSenderThread start. interval: "+ TopologyManager.LLDP_INTERVAL);
		this.lldpSenderThread.startTimer();
		return;
	}

	private void startLldpCheckerThread(){
		System.out.println("LldpCheckerThread start.interval: "+ TopologyManager.LLDP_INTERVAL);
		this.lldpCheckerThread.startTimer();
		return;
	}

	private void startTableUpdaterThread(){
		System.out.println("TableManagerThread start.");
		this.tableUpdaterThread.startTimer();
		return;
	}
}
