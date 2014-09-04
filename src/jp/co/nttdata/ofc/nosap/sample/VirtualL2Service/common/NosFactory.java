package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common;

import jp.co.nttdata.ofc.common.except.NosException;
import jp.co.nttdata.ofc.common.util.IpAddress;
import jp.co.nttdata.ofc.common.util.MacAddress;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.exception.NosapRuntimeException;

public class NosFactory {

	public static MacAddress createMacAddress()
	{
		return new MacAddress();
	}

	public static MacAddress createMacAddress(byte[] macaddr)
	{
		try
		{
			return new MacAddress(macaddr);
		}
		catch(NosException e)
		{
			throw new NosapRuntimeException("The mac address is invalid. macaddr: " + macaddr, e);
		}
	}

	public static MacAddress createMacAddress(String macaddr)
	{
		try
		{
			return new MacAddress(macaddr);
		}
		catch(NosException e)
		{
			throw new NosapRuntimeException("The mac address is invalid. macaddr: " + macaddr, e);
		}

	}

	public static MacAddress createMacAddress(long macaddr)
	{
		try
		{
			return new MacAddress(macaddr);
		}
		catch(NosException e)
		{
			throw new NosapRuntimeException("The mac address is invalid. macaddr: " + macaddr, e);
		}
	}

	public static MacAddress createMacAddress(MacAddress macaddr)
	{
		try
		{
			return new MacAddress(macaddr);
		}
		catch(NosException e)
		{
			throw new NosapRuntimeException("The mac address is invalid. macaddr: " + macaddr, e);
		}
	}

	public static IpAddress createIpAddress()
	{
		return new IpAddress();
	}

	public static IpAddress createIpAddress(String ipaddr)
	{
		try
		{
			return new IpAddress(ipaddr);
		}
		catch(NosException e)
		{
			throw new NosapRuntimeException("The mac address is invalid. ipaddr: " + ipaddr, e);
		}
	}

	public static IpAddress createIpAddress(long ipaddr)
	{
		try
		{
			return new IpAddress(ipaddr);
		}
		catch(NosException e)
		{
			throw new NosapRuntimeException("The mac address is invalid. ipaddr: " + ipaddr, e);
		}
	}

	public static IpAddress createIpAddress(byte[] ipaddr)
	{
		try
		{
			return new IpAddress(ipaddr);
		}
		catch(NosException e)
		{
			throw new NosapRuntimeException("The mac address is invalid. ipaddr: " + ipaddr, e);
		}
	}

	public static IpAddress createIpAddress(IpAddress ipaddr)
	{
		try
		{
			return new IpAddress(ipaddr);
		}
		catch(NosException e)
		{
			throw new NosapRuntimeException("The mac address is invalid. ipaddr: " + ipaddr, e);
		}
	}

}
