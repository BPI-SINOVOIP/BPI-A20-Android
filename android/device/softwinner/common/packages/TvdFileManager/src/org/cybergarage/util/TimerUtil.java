package org.cybergarage.util;

public final class TimerUtil
{
	/** �����̵߳�˯��
	 * @param waitTime ˯��ʱ��
	 *  
	 */
	public final static void wait(int waitTime)
	{
		try {
			Thread.sleep(waitTime);
		}
		catch (Exception e) {}
	}

	/** �߳����ʱ��˯��
	 * @param  time ˯��ʱ�������� �����̵߳�˯��ʱ��
	 */
	public final static void waitRandom(int time)
	{
		int waitTime = (int)(Math.random() * time);		
		try {
			Thread.sleep(waitTime);
		}
		catch (Exception e) {}
	}
}

