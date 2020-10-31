package com.sulistionoadi.ngoprek.common.config.dto;

import java.util.Date;

import com.sulistionoadi.ngoprek.common.dto.BaseMasterDTO;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class GeneralConfigDTO extends BaseMasterDTO {
	
	private static final long serialVersionUID = 4872661622091145563L;
	private String configCode;
	private String configValue;
	
	@Builder
	public GeneralConfigDTO(Long id, String createdBy, Date createdDate, String updatedBy, Date updatedDate,
			Boolean isDeleted, Boolean isActive, String configCode, String configValue) {
		super(id, createdBy, createdDate, updatedBy, updatedDate, isDeleted, isActive);
		this.configCode = configCode;
		this.configValue = configValue;
	}
	
}
