package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.path;

import java.util.LinkedList;

import jp.co.nttdata.ofc.common.util.MacAddress;

public class PathManager {
	private static PathManager pathManager = new PathManager();

	private LinkedList<Path> pathList;

	private PathManager()
	{
		pathList = new LinkedList<Path>();
	}

	public static PathManager getInstance() {
		return pathManager;
	}

	/**
	 * 全通信パスから指定したMacAddressを{@linkplain Path}.macsに含む要素を全て削除する。
	 *
	 * @param mac 削除する要素を調べる値
	 */
	public synchronized void removeElements(MacAddress mac)
	{
		LinkedList<Path> containsList = this.getPathList(mac);

		pathList.removeAll(containsList);
	}

	public synchronized void removeElements(Long dpid1, int port1, Long dpid2, int port2)
	{
		LinkedList<Path> containsList = this.getPathList(dpid1, dpid2, port1, port2);

		pathList.removeAll(containsList);
	}

	/**
	 * 全通信パスから指定したMacAddressを含む通信パスリストを返却する。
	 * 返却するリストはシャローコピーであるため、リストに含まれる要素の変更はコピー元に影響を与える。
	 * @param mac 要素を調べる値
	 * @return 指定したMacAddressを含む通信パスリスト
	 */
	public LinkedList<Path> getPathList(MacAddress mac)
	{
		LinkedList<Path> containsList = new LinkedList<Path>();

		for(Path path : pathList){
			if(path.contains(mac)){
				containsList.add(path);
			}
		}
		return containsList;
	}

	public LinkedList<Path> getPathList(Long dpid1, Long dpid2, int port1, int port2)
	{
		LinkedList<Path> list = new LinkedList<Path>();
		for(Path path : pathList){
			if(path.contains(dpid1, port1, dpid2, port2)){
				list.add(path);
			}
		}
		return list;
	}

	public LinkedList<Path> getPathList(long dpid1, long dpid2){
		LinkedList<Path> ret = new LinkedList<Path>();

		for(Path path : pathList){
			if(path.contains(dpid1, dpid2)){
				ret.add(path);
			}
		}

		return ret;
	}


	public boolean addPath(Path path){
		MacAddress[] macs = path.getMacs();

		for(Path p : pathList){
			MacAddress[] m = p.getMacs();
			if(macs[0] == null){
				if((m[0] == null) && macs[1].equals(m[1])){
					pathList.remove(p);
					break;
				}
			}
			else if(macs[1] == null){
				if((m[1] == null) && macs[0].equals(m[0])){
					pathList.remove(p);
					break;
				}
			}
			else{
				if(macs[0].equals(m[0]) && macs[1].equals(m[1])){
					pathList.remove(p);
					break;
				}
			}
		}

		return pathList.add(path);
	}

	/**
	 * 通信パスリストのシャローコピーを返却する。
	 * @return 通信パスリストのシャローコピー
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<Path> getPathList() {
		return (LinkedList<Path>) pathList.clone();
	}
}
