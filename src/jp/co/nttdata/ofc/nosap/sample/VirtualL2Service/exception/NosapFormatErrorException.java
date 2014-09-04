package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.exception;

import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.ErrorCode;

public class NosapFormatErrorException extends NosapRuntimeException
{
	private static final long serialVersionUID = 1L;

	public NosapFormatErrorException(String msg)
	{
		super(msg);
	}

	public NosapFormatErrorException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	@Override
	public long getErrorCode()
	{
		return ErrorCode.FORMAT_ERROR;
	}
}
