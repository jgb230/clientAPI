package clientAPI;

import java.util.Formatter;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RWLock {
	public static Lock m_lock = new ReentrantLock(); 
	public static Condition m_rcond = m_lock.newCondition();
	public static Condition m_wcond = m_lock.newCondition();
	public static int read_cnt = 0;
	public static int write_cnt = 0;
	public static boolean inwriteflag = false ;
	
	public void lock_read() throws InterruptedException {
		m_lock.lock();
		while (write_cnt != 0) {
			m_rcond.await();
		}
		++read_cnt;
		m_lock.unlock();
	}
	
	public void lock_write() throws InterruptedException
	{
		m_lock.lock();
		++write_cnt;
		while(read_cnt != 0 || inwriteflag) {
			m_wcond.await();
		}
		inwriteflag = true;
		m_lock.unlock();
	}
	
	public void release_read()
	{
		m_lock.lock();
		if (--read_cnt == 0 && write_cnt > 0)
		{
			m_wcond.signal();
		}

		m_lock.unlock();
	}
	
	public void release_write()
	{
		m_lock.lock();
		if (--write_cnt == 0)
		{
			m_rcond.signalAll();
		}
		else
		{
			m_wcond.signal();
		}
		inwriteflag = false;
		m_lock.unlock();
	}
	
	public void print() {
		System.out.println(this);
		System.out.println(m_lock);
		System.out.println(m_rcond);
		System.out.println(m_wcond);
		System.out.println(read_cnt);
		System.out.println(write_cnt);
	}
	
	public static void LOG(String format, Object... args) {
    	System.out.println(new Formatter().format(format, args).toString());
    }
}
