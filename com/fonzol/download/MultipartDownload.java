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
	//�������ص��߳���
	public static int threadcount = 3;
	//�����̼߳���
	public static int runningthreadCount;
	//�������ļ�·��http://192.168.1.118:8080/1.gif
	public static String path = "http://192.168.1.118:8080/1.gif";
	/**
	 * @param args
	 * @throws Exception 
	 */
	
	public static void main(String[] args) throws Exception {
		
		
		URL url  = new URL(path);
		//������
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		//�������ӵĲ���
		conn.setConnectTimeout(5000);
		conn.setRequestMethod("GET");
		//��ȡ������״̬��
		int code = conn.getResponseCode();
		if (code == 200) {
			int length = conn.getContentLength();
			System.out.println(length);
			//���������ļ�
			RandomAccessFile raf = new RandomAccessFile(getFileName(path), "rw");
			raf.setLength(length);
			raf.close();
			//����߳�������ÿ���̵߳������С
			int blocksize = length / threadcount;
			//�����߳�֮ǰ��������߳���
			runningthreadCount = threadcount;
			for(int threadID = 0;threadID < threadcount;threadID ++) {
				int startIndex = threadID * blocksize;
				int endIndex = (threadID + 1 ) * blocksize -1;
				//���һ������
				if (threadID == (threadcount - 1 )) {
					endIndex = length - 1;
				}
				System.out.println("ÿ�����鷶Χ : " + startIndex + "--" + endIndex);
				new downloadThread(threadID, startIndex, endIndex).start();
			}
		}
	}
	public static class downloadThread extends Thread {
		/**
		 * �߳�id
		 */
		private int threadId;
		/**
		 * �߳�������ʼλ��
		 */
		private int startIndex;
		/**
		 * ʵ�����ص���λ��
		 */
		private int currentPosition;
		/**
		 * �߳̽���λ��
		 */
		private int endIndex;
		public downloadThread(int threadId, int startIndex, int endIndex) {
			this.threadId = threadId;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}
		/**
		 * �ļ������߼�
		 */
		public void run() {
			//System.out.println("�����߳�--" + threadId);
			try {
				//��ȡ����������
				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				//�����ñ��ؿؼ�
				RandomAccessFile raf = new RandomAccessFile(getFileName(path), "rw");
				//���ж����ؽ���,���ݽ��������������ط�Χ
				File info = new File(threadId + ".position");
				if (info.exists() && info.length() > 0) {	//λ���ļ�����
					FileInputStream fis = new FileInputStream(info);
					//�����ļ���һ������
					BufferedReader br = new BufferedReader(new InputStreamReader(fis));
					currentPosition = Integer.valueOf(br.readLine());
					conn.setRequestProperty("Range", "bytes=" + currentPosition + "-" + endIndex);
					System.out.println(threadId + "�����ϴ�����");
					fis.close();
					//�ҵ����߳�д���ݵĿ�ʼλ��
					raf.seek(currentPosition);
				}else {
					
					//����ֻ�������ڱ��̵߳���һ��������
					conn.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);
					//�ҵ����߳�д���ݵĿ�ʼλ��
					raf.seek(startIndex);
				}
				InputStream is = conn.getInputStream();
				byte[] b = new byte[1024];
				int len;
				
				
				while((len = is.read(b)) != -1) {
					//System.out.println("�߳�" + threadId + "--" + new String(b).length());
					//������д�����̶߳�Ӧ��λ����
					raf.write(b, 0, len);
					//��¼����д���ļ���λ��
					currentPosition += len;
					//�����ļ��������λ������
					File file = new File(threadId + ".position");
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(String.valueOf(currentPosition).getBytes());
					fos.close();
				}
				raf.close();
				is.close();
				System.out.println(threadId + "���߳��������");
				//��ǰ������ɽ�λ���ļ�����
				File ff = new File(threadId + ".position");
				ff.renameTo(new File(threadId + ".position.finish"));
				//�������,����һ���߳�
				synchronized (MultipartDownload.class) {
					
					runningthreadCount -- ;
					if (runningthreadCount <= 0) {
						for(int i = 0; i < threadcount; i ++) {
							//ɾ��λ�ü�¼�ļ�
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
	 * ͨ���ļ�·����ȡ�ļ���
	 * @param path
	 * @return filename
	 */
	public static String getFileName(String path) {
		String fileName = path.substring(path.lastIndexOf("/") + 1);
		return fileName;
	}

}
