package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.exception;

import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.ErrorCode;

public class NosapRuntimeException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public NosapRuntimeException(String msg)
	{
		super(msg);
	}

	public NosapRuntimeException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public long getErrorCode()
	{
		return ErrorCode.UNKNOWN;
	}
}
