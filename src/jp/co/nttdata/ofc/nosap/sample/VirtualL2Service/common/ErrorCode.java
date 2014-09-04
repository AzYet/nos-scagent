package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common;

public interface ErrorCode {
	public final long INVALID_PARAMETERS = 0x1L;
	public final long NOT_FOUND          = 0x2L;
	public final long DUPLICATION        = 0x3L;
	public final long DEPENDENCY         = 0x4L;
	public final long LOGIC              = 0x5L;
	public final long IO                 = 0x6L;
	public final long FORMAT_ERROR       = 0x7L;
	public final long ILLEGAL_ARGUMENT   = 0x8L;
	public final long OUT_OF_RANGE       = 0x9L;
	public final long INTERRUPTED        = 0xaL;
	public final long NULL		         = 0xbL;
	public final long NOT_IMPLEMENT      = 0xeeeeeeeeeeeeeeeeL;
	public final long UNKNOWN            = 0xffffffffffffffffL;
}
