package com.ioman.counter.timer;

import com.ioman.counter.entity.TimerPanel;
import com.ioman.counter.util.AudioPlayer;
import com.ioman.counter.util.FileUtils;
import org.joda.time.DateTime;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * <p>Title: com.ioman.counter</p>
 * <p/>
 * <p>
 * Description: 计时计算线程
 * </p>
 * <p/>
 *
 * @author Lynn
 *         CreateTime：6/9/17
 */
public class TimerCounter implements Runnable, ActionListener{
	
	private TimerPanel timerPanel;
	private long leftSec;//剩余秒数
	private long usedSec;//已经走的秒数
	private boolean isRunning;//是否在计时
	private boolean isAlarm;
	private JFrame frame;
	private String title;
	private final int BEGIN_FLAG = 0;
	private final int END_FLAG = 1;
	
	public TimerCounter(TimerPanel timerPanel, JFrame frame) {
		this.timerPanel = timerPanel;
		this.frame = frame;
		this.title = timerPanel.getTitle().getText();
	}
	
	/**
	 * When an object implementing interface <code>Runnable</code> is used
	 * to create a thread, starting the thread causes the object's
	 * <code>run</code> method to be called in that separately executing
	 * thread.
	 * <p>
	 * The general contract of the method <code>run</code> is that it may
	 * take any action whatsoever.
	 *
	 * @see Thread#run()
	 */
	public void run() {
		
		init();
		
		addToListener();
		
		if(isRunning){
			new Thread(new TickTickRunner()).start();
		}
	}
	
	private void reset(){
		
		//设置总秒数
		resetSec();
		
		setButtonStatus();
		
		//重置时间提示文本
		resetTimeText();
	}
	
	private void init(){
		
		initSec();
		
		setButtonStatus();
		
		timerPanel.getTimeLeftDetail().setText(formatSecond(leftSec));
		timerPanel.getTimeUsedDetail().setText(formatSecond(usedSec));
	}
	
	private void resetTimeText(){
		
		if(isRunning) return;
		
		timerPanel.getTimeLeftDetail().setText(formatSecond(leftSec));
		timerPanel.getTimeUsedDetail().setText(formatSecond(usedSec));
	}
	
	//reset the time
	private void resetSec(){
		
		if(isRunning) return;
		
		usedSec = 0;
		leftSec = countLeftSec();
	}
	
	private long countLeftSec(){
		
		long totalSec = 0;
		
		//统计小时
		if(timerPanel.getZeroHour().isSelected()){
			totalSec += 0;
		}else if(timerPanel.getOneHour().isSelected()){
			totalSec += 1 * 60 * 60;
		}else if(timerPanel.getTwoHour().isSelected()){
			totalSec += 2 * 60 * 60;
		}else {
			totalSec += 3 * 60 * 60;
		}
		
		int minute = Integer.parseInt(timerPanel.getMinuteComboBox().getSelectedItem().toString());
		
		if(minute > 0) {
			totalSec += minute * 60;
		}
		
		return totalSec;
	}
	
	//first initialization time
	private void initSec(){
		
		if(isRunning) return;
		
		try {
			String timeInfo = FileUtils.read(title);//初始化
			String[] arr = timeInfo.split("#");
			
			long startTimeMillis = Long.parseLong(arr[0]);
			long endTimeMillis = Long.parseLong(arr[1]);
			long currentTimeMillis = System.currentTimeMillis();
			
			if(currentTimeMillis >= endTimeMillis){
				reset();
				return;
			}
			
			usedSec = (currentTimeMillis - startTimeMillis)/1000;
			leftSec = (endTimeMillis - currentTimeMillis)/1000;
			
		} catch (Exception e) {
			usedSec = 0;
			leftSec = 0;
		}
		
		if(leftSec > 0){//剩余时间超过10秒，采用储存的时间
			isRunning = true;
			return ;
		}
		
		//统计小时
		usedSec = 0;
		leftSec = countLeftSec();
	}
	
	private void addToListener(){
		timerPanel.getStart().addActionListener(this);
		timerPanel.getPause().addActionListener(this);
		timerPanel.getStop().addActionListener(this);
		
		timerPanel.getZeroHour().addActionListener(this);
		timerPanel.getOneHour().addActionListener(this);
		timerPanel.getTwoHour().addActionListener(this);
		timerPanel.getThreeHour().addActionListener(this);
		
		timerPanel.getMinuteComboBox().addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (ItemEvent.SELECTED == e.getStateChange()) {
					
					reset();
				}
			}
		});
	}
	
	private void disable(){
		timerPanel.getZeroHour().setEnabled(false);
		timerPanel.getOneHour().setEnabled(false);
		timerPanel.getTwoHour().setEnabled(false);
		timerPanel.getThreeHour().setEnabled(false);
		timerPanel.getMinuteComboBox().setEnabled(false);
	}
	
	private void enable(){
		timerPanel.getZeroHour().setEnabled(true);
		timerPanel.getOneHour().setEnabled(true);
		timerPanel.getTwoHour().setEnabled(true);
		timerPanel.getThreeHour().setEnabled(true);
		timerPanel.getMinuteComboBox().setEnabled(true);
	}
	
	/**
	 * Invoked when an action occurs.
	 *
	 * @param e
	 */
	public void actionPerformed(ActionEvent e) {
		
		if(e.getSource() == timerPanel.getStart()){
			
			if(!isRunning && leftSec > 0){
				
				isRunning = true;//开始计时
				this.writeToDB(BEGIN_FLAG);
				new Thread(new TickTickRunner()).start();
				setButtonStatus();
			}
			
		}else if(e.getSource() == timerPanel.getPause()) {
			
			if(isRunning){
				isRunning = false;
				this.writeToDB(END_FLAG);
				setButtonStatus();
				timerPanel.getStop().setEnabled(true);
				disable();
			}
			
			
		}else if(e.getSource() == timerPanel.getStop()){
			
			int status = JOptionPane.showConfirmDialog(frame, "确定要结束?", title, JOptionPane.YES_NO_OPTION);
			
			if(status == 0) {
				
				isRunning = false;
				isAlarm = false;
				leftSec = 0;
				usedSec = 0;
				
				reset();
				
				this.writeToDB(END_FLAG);
			}
			
		}else if(e.getSource() == timerPanel.getZeroHour()){
			
			reset();
		}else if(e.getSource() == timerPanel.getOneHour()) {
			
			reset();
		}else if(e.getSource() == timerPanel.getTwoHour()) {
			
			reset();
		}else if(e.getSource() == timerPanel.getThreeHour()){
			
			reset();
		}
	}
	
	public void setButtonStatus(){
		
		int fontSize = 14;
		int style = 0;
		
		if(isRunning){//如果正在计时中
			
			timerPanel.getTimeUsedDetail().setFont(new Font("Dialog", 1, 21));
			timerPanel.getTimeLeftDetail().setFont(new Font("Dialog", 1, 21));
			
			timerPanel.getStart().setEnabled(false);
			timerPanel.getPause().setEnabled(true);
			timerPanel.getStop().setEnabled(true);
			
			disable();
			
		}else {
			
			if(usedSec > 0){
				fontSize = 21;
				style = 1;
			}
			
			timerPanel.getTimeUsedDetail().setFont(new Font("Dialog", style, fontSize));
			timerPanel.getTimeLeftDetail().setFont(new Font("Dialog", style, fontSize));
			
			timerPanel.getStart().setEnabled(true);
			timerPanel.getPause().setEnabled(false);
			timerPanel.getStop().setEnabled(false);
			
			enable();
		}
	}
	
	/**
	 * 将秒转换成时间格式 "0 时 00 分 00 秒"
	 * @param seconds
	 * @return
	 */
	private String formatSecond(long seconds){
		
		long hour = seconds / 60 / 60;
		long min = (seconds - (hour * 60 * 60)) / 60;
		long sec = seconds - (hour * 60 * 60) - (min * 60);
		
		String minStr = "" + min;
		if(min < 10){
			minStr = "0" + min;
		}
		
		String secStr = "" + sec;
		if(sec < 10){
			secStr = "0" + sec;
		}
	
		return hour + " 时 " + minStr + " 分 " + secStr + " 秒 ";
	}
	
	private void writeToDB(int flag){
		
		if(flag == BEGIN_FLAG){//开始计时
			
			long startTimeMillis = System.currentTimeMillis();
			long endTimeMillis = startTimeMillis + (leftSec * 1000);
			
			FileUtils.write(title, startTimeMillis + "#" + endTimeMillis);
			
		}else if(flag == END_FLAG){//清除计时
			
			FileUtils.write(title, "");
		}
		
	}
	
	class TickTickRunner implements Runnable {
		
		/**
		 * When an object implementing interface <code>Runnable</code> is used
		 * to create a thread, starting the thread causes the object's
		 * <code>run</code> method to be called in that separately executing
		 * thread.
		 * <p>
		 * The general contract of the method <code>run</code> is that it may
		 * take any action whatsoever.
		 *
		 * @see Thread#run()
		 */
		public void run() {
			
			System.out.println("TickTick alive ... ");
			
			//时间还在，并且需要跑的时候
			while(isRunning && leftSec > 0){
				
				leftSec--;
				usedSec++;
				
				String leftTimeText = formatSecond(leftSec);
				String usedTimeText = formatSecond(usedSec);
				timerPanel.getTimeLeftDetail().setText(leftTimeText);
				timerPanel.getTimeUsedDetail().setText(usedTimeText);
				
				if (leftSec == 0){
					//播放声音
					isAlarm = true;
					new Thread(new AlarmAudio()).start();
					
//					JOptionPane.showMessageDialog(frame, timerPanel.getTitle().getText() + " 时间到！！！");
					int status = JOptionPane.showConfirmDialog(frame, timerPanel.getTitle().getText() + " 时间到！！！", title, JOptionPane.CLOSED_OPTION);
					if(status == 0){
						isAlarm = false;
					}
					isRunning = false;
					
					reset();
					
					writeToDB(END_FLAG);
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
				}
			}
			
			System.out.println("TickTick die ...");
		}
	}
	
	class AlarmAudio implements Runnable {
		
		/**
		 * When an object implementing interface <code>Runnable</code> is used
		 * to create a thread, starting the thread causes the object's
		 * <code>run</code> method to be called in that separately executing
		 * thread.
		 * <p>
		 * The general contract of the method <code>run</code> is that it may
		 * take any action whatsoever.
		 *
		 * @see Thread#run()
		 */
		public void run() {
			
			System.out.println("AlarmPlayer alive ... ");
			
			while (isAlarm){
				new AudioPlayer().run();
			}
		}
	}
}
