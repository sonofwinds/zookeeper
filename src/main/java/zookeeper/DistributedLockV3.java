package zookeeper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class DistributedLockV3 implements Watcher,Lock{
	private ZooKeeper zk = null;
	private String znodePath = "/exclusiveLock";
	private String znodeName = "lock_";
	private String connectString = "10.9.193.59:2181";
	private int sessionTimeOut = 5000;
	private String lockPath = null;
	private String threadName = Thread.currentThread().getName();
	private CountDownLatch latch = new CountDownLatch(1);
	public DistributedLockV3() {
		try {
			zk = new ZooKeeper(connectString, sessionTimeOut, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void process(WatchedEvent event) {
		System.out.println("已经watch:" + event.getPath());
	}
	
	//阻塞公平锁
	public void lock() {
		createNode();
		attempBlockLock();
	}
	
	public void lockInterruptibly() throws InterruptedException {
		
	}
	
	//非阻塞非公平锁
	public boolean tryLock() {
		createNode();
		return attempNoBlockLock();
	}
	
	//超时时间内获取锁
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		createNode();
		return attempWaitForBlockLock(time, unit);
	}
	
	public void unlock() {
          releaseLock();		
	}
	
	public Condition newCondition() {
		return null;
	}
	
	private void createNode() {
		try {
			lockPath = zk.create(znodePath + "/" + znodeName, threadName.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
			System.out.println(threadName + " createNode:lockPath=" + lockPath);
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	private void attempBlockLock() {
		try {
			List<String> childrens = zk.getChildren(znodePath, false);
			Collections.sort(childrens);
			int index = childrens.indexOf(lockPath.substring(znodePath.length() + 1));
			if(index == 0) {//创建的znode是首节点
				System.out.println(threadName + ":已拿到锁");
				return;
			}
			//监控上一个节点
			String prevZnodePath = childrens.get(index - 1);
			Watcher watcher = new Watcher() {
				public void process(WatchedEvent event) {
					System.out.println(threadName + ":被唤醒,prevPath=" + event.getPath());
					latch.countDown();
				}
			};
			Stat stat = zk.exists(znodePath + "/" +prevZnodePath, watcher);
			if(stat == null) {
				attempBlockLock();
			}else {
				latch.await();
				attempBlockLock();
			}
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private boolean attempNoBlockLock() {
		try {
			List<String> childrens = zk.getChildren(znodePath, false);
			Collections.sort(childrens);
			int index = childrens.indexOf(lockPath.substring(znodePath.length() + 1));
			if(index == 0) {//创建的znode是首节点
				System.out.println(threadName + ":已拿到锁");
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private boolean attempWaitForBlockLock(long timeout,TimeUnit unit) {
		try {
			List<String> childrens = zk.getChildren(znodePath, false);
			Collections.sort(childrens);
			int index = childrens.indexOf(lockPath.substring(znodePath.length() + 1));
			if(index == 0) {//创建的znode是首节点
				System.out.println(threadName + ":已拿到锁");
				return true;
			}
			//监控上一个节点
			String prevZnodePath = childrens.get(index - 1);
			Watcher watcher = new Watcher() {
				public void process(WatchedEvent event) {
					System.out.println(threadName + ":被唤醒,prevPath=" + event.getPath());
					latch.countDown();
				}
			};
			Stat stat = zk.exists(znodePath + "/" +prevZnodePath, watcher);
			if(stat == null) {
				return attempWaitForBlockLock(timeout, unit);
			}else {
				boolean flag = latch.await(timeout, unit);
				if(flag) {
					return attempWaitForBlockLock(timeout, unit);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private void releaseLock() {
		try {
			if(zk != null)
				System.out.println(threadName + ":释放锁" + lockPath);
			zk.delete(lockPath, -1);
			zk.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

