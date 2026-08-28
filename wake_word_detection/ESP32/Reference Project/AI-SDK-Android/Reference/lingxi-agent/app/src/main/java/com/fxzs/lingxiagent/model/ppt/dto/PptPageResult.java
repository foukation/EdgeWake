package com.fxzs.lingxiagent.model.ppt.dto;

import java.util.List;

/**
 * 分页结果
 */
public class PptPageResult<T> {
	private List<T> list;
	private Long total;

	public List<T> getList() {
		return list;
	}

	public void setList(List<T> list) {
		this.list = list;
	}

	public Long getTotal() {
		return total;
	}

	public void setTotal(Long total) {
		this.total = total;
	}

}