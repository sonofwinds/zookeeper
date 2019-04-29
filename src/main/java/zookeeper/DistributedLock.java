package zookeeper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class DistributedLock implements Watcher{
	private ZooKeeper zk = null;
	private String znodePath = "/exclusiveLock";
	private String znodeName = "lock_";
	private String connectString = "10.9.193.59:2181";
	private int sessionTimeOut = 5000;
	private String lockPath = null;
	private String threadName = Thread.currentThread().getName();
	public DistributedLock() {
		try {
			zk = new ZooKeeper(connectString, sessionTimeOut, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void process(WatchedEvent event) {
		System.out.println("已经watch:" + event.getPath());
	}
	public void tryLock() {
		//创建节点
		createNode();
		//尝试获取锁
		attempLock();
	}
	public void createNode() {
		try {
			lockPath = zk.create(znodePath + "/" + znodeName, threadName.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
		    System.out.println(threadName + " createNode:lockPath=" + lockPath);
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	public void attempLock() {
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
					synchronized(this) {
                        System.out.println(threadName + ":被唤醒,prevPath=" + event.getPath());
						this.notifyAll();
					}	   
				}
			};
			Stat stat = zk.exists(znodePath + "/" +prevZnodePath, watcher);
			if(stat == null) {
				attempLock();
			}else {
				synchronized(watcher) {
					System.out.println(threadName + ":被阻塞");
					watcher.wait();
				}
				attempLock();
			}
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	public void releaseLock() {
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
