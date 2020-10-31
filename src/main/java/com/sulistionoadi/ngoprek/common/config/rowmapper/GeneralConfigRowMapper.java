package com.sulistionoadi.ngoprek.common.config.rowmapper;

import static com.sulistionoadi.ngoprek.common.utils.RowMapperUtils.getBooleanValue;
import static com.sulistionoadi.ngoprek.common.utils.RowMapperUtils.getDateValue;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.sulistionoadi.ngoprek.common.config.dto.GeneralConfigDTO;

public class GeneralConfigRowMapper implements RowMapper<GeneralConfigDTO> {

	@Override
	public GeneralConfigDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
		Boolean isDeleted = getBooleanValue(rs, rowNum, "is_deleted");
		Boolean isActive = getBooleanValue(rs, rowNum, "is_active");
		
		return GeneralConfigDTO.builder()
				.id(rs.getLong("id"))
				.createdDate(getDateValue(rs, rowNum, "created_date"))
				.createdBy(rs.getString("created_by"))
				.updatedDate(getDateValue(rs, rowNum, "updated_date"))
				.updatedBy(rs.getString("updated_by"))
				.isActive(isActive)
				.isDeleted(isDeleted)
				.configCode(rs.getString("config_code"))
				.configValue(rs.getString("config_value"))
				.build();
	}

}
