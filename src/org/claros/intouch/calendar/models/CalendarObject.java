package org.claros.intouch.calendar.models;

import java.sql.Timestamp;

public class CalendarObject implements Cloneable {
	private Long id;
	private String username;
	private Timestamp recordDate;
	private Integer repeatType;
	private String category;
	private String description;
	private Integer reminderDays;

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Timestamp getRecordDate() {
		return recordDate;
	}

	public void setRecordDate(Timestamp recordDate) {
		this.recordDate = recordDate;
	}

	public Integer getReminderDays() {
		return reminderDays;
	}

	public void setReminderDays(Integer reminderDays) {
		this.reminderDays = reminderDays;
	}

	public Integer getRepeatType() {
		return repeatType;
	}

	public void setRepeatType(Integer repeatType) {
		this.repeatType = repeatType;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) { // this shouldn't happen, since we are Cloneable
			return null;
		}
	}
}
