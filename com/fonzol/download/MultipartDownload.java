package com.fonzol.download;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class MultipartDownload {
	//设置下载的线程数
	public static int threadcount = 3;
	//设置线程计数
	public static int runningthreadCount;
	//服务器文件路径http://192.168.1.118:8080/1.gif
	public static String path = "http://192.168.1.118:8080/1.gif";
	/**
	 * @param args
	 * @throws Exception 
	 */
	
	public static void main(String[] args) throws Exception {
		
		
		URL url  = new URL(path);
		//打开连接
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		//设置连接的参数
		conn.setConnectTimeout(5000);
		conn.setRequestMethod("GET");
		//获取服务器状态码
		int code = conn.getResponseCode();
		if (code == 200) {
			int length = conn.getContentLength();
			System.out.println(length);
			//创建本地文件
			RandomAccessFile raf = new RandomAccessFile(getFileName(path), "rw");
			raf.setLength(length);
			raf.close();
			//多个线程下载是每个线程的区块大小
			int blocksize = length / threadcount;
			//开启线程之前设置最大线程数
			runningthreadCount = threadcount;
			for(int threadID = 0;threadID < threadcount;threadID ++) {
				int startIndex = threadID * blocksize;
				int endIndex = (threadID + 1 ) * blocksize -1;
				//最后一个区块
				if (threadID == (threadcount - 1 )) {
					endIndex = length - 1;
				}
				System.out.println("每个区块范围 : " + startIndex + "--" + endIndex);
				new downloadThread(threadID, startIndex, endIndex).start();
			}
		}
	}
	public static class downloadThread extends Thread {
		/**
		 * 线程id
		 */
		private int threadId;
		/**
		 * 线程理论起始位置
		 */
		private int startIndex;
		/**
		 * 实际下载到的位置
		 */
		private int currentPosition;
		/**
		 * 线程结束位置
		 */
		private int endIndex;
		public downloadThread(int threadId, int startIndex, int endIndex) {
			this.threadId = threadId;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}
		/**
		 * 文件下载逻辑
		 */
		public void run() {
			//System.out.println("启动线程--" + threadId);
			try {
				//获取服务器连接
				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				//创建好本地控件
				RandomAccessFile raf = new RandomAccessFile(getFileName(path), "rw");
				//先判断下载进度,根据进度设置区块下载范围
				File info = new File(threadId + ".position");
				if (info.exists() && info.length() > 0) {	//位置文件存在
					FileInputStream fis = new FileInputStream(info);
					//读该文件的一行数据
					BufferedReader br = new BufferedReader(new InputStreamReader(fis));
					currentPosition = Integer.valueOf(br.readLine());
					conn.setRequestProperty("Range", "bytes=" + currentPosition + "-" + endIndex);
					System.out.println(threadId + "继续上次下载");
					fis.close();
					//找到本线程写数据的开始位置
					raf.seek(currentPosition);
				}else {
					
					//设置只下载属于本线程的那一部分数据
					conn.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);
					//找到本线程写数据的开始位置
					raf.seek(startIndex);
				}
				InputStream is = conn.getInputStream();
				byte[] b = new byte[1024];
				int len;
				
				
				while((len = is.read(b)) != -1) {
					//System.out.println("线程" + threadId + "--" + new String(b).length());
					//把数据写到本线程对应的位置上
					raf.write(b, 0, len);
					//记录数据写到文件的位置
					currentPosition += len;
					//创建文件保存这个位置数据
					File file = new File(threadId + ".position");
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(String.valueOf(currentPosition).getBytes());
					fos.close();
				}
				raf.close();
				is.close();
				System.out.println(threadId + "号线程下载完成");
				//当前下载完成将位置文件改名
				File ff = new File(threadId + ".position");
				ff.renameTo(new File(threadId + ".position.finish"));
				//下载完成,减少一个线程
				synchronized (MultipartDownload.class) {
					
					runningthreadCount -- ;
					if (runningthreadCount <= 0) {
						for(int i = 0; i < threadcount; i ++) {
							//删除位置记录文件
							File f = new File(i + ".position.finish");
							f.delete();
						}
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
	}
	/**
	 * 通过文件路径获取文件名
	 * @param path
	 * @return filename
	 */
	public static String getFileName(String path) {
		String fileName = path.substring(path.lastIndexOf("/") + 1);
		return fileName;
	}

}
