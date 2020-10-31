package com.sulistionoadi.ngoprek.common.config.dao;

import java.util.Optional;

import com.sulistionoadi.ngoprek.common.config.dto.GeneralConfigDTO;
import com.sulistionoadi.ngoprek.common.dao.BaseDao;

public interface GeneralConfigDao extends BaseDao<GeneralConfigDTO, Long>{

	public Optional<GeneralConfigDTO> findByCode(String code);
	
}
