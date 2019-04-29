package zookeeper.test;

import java.util.concurrent.TimeUnit;

import zookeeper.DistributedLockV2;
import zookeeper.DistributedLockV3;

public class TestLock {

	public static void main(String[] args) {
        for(int i = 0 ; i < 4 ;i++) {
          new Thread(new MyRunnableV4()).start();
        } 
         
	}

}
class MyRunnable implements Runnable{

	public void run() {
		DistributedLockV2 distributedLock = new DistributedLockV2();
		try {
			distributedLock.tryLock();
			System.out.println(Thread.currentThread().getName() + " excute.....");
			Thread.sleep(2000);//执行业务逻辑
			System.out.println(Thread.currentThread().getName() + " done");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}finally {
			distributedLock.releaseLock();
		}
	}
	
}
class MyRunnableV2 implements Runnable{

	public void run() {
		DistributedLockV3 distributedLock = new DistributedLockV3();
		try {
			boolean isAcquire = distributedLock.tryLock();
			if(isAcquire) {
				System.out.println(Thread.currentThread().getName() + " excute.....");
				Thread.sleep(2000);//执行业务逻辑
				System.out.println(Thread.currentThread().getName() + " done");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}finally {
			distributedLock.unlock();
		}
	}
	
}
class MyRunnableV3 implements Runnable{
	
	public void run() {
		DistributedLockV3 distributedLock = new DistributedLockV3();
		try {
			distributedLock.lock();
			System.out.println(Thread.currentThread().getName() + " excute.....");
			Thread.sleep(2000);//执行业务逻辑
			System.out.println(Thread.currentThread().getName() + " done");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}finally {
			distributedLock.unlock();
		}
	}
	
}

class MyRunnableV4 implements Runnable{
	
	public void run() {
		DistributedLockV3 distributedLock = new DistributedLockV3();
		try {
			//等待获取锁的超时时间是4秒钟
			boolean isAcquire = distributedLock.tryLock(1, TimeUnit.SECONDS);
			if(isAcquire) {
				System.out.println(Thread.currentThread().getName() + " excute.....");
				Thread.sleep(2000);//执行业务逻辑
				System.out.println(Thread.currentThread().getName() + " done");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}finally {
			distributedLock.unlock();
		}
	}
	
}