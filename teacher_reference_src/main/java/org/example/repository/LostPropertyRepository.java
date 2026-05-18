package org.example.repository;

import org.example.enitity.LostPropertyEntity;

import java.util.List;

public interface LostPropertyRepository extends  BaseRepository<LostPropertyEntity>{
    /**
     * 根据手机号查询找到失物登记信息
     * @param phone
     * @return
     */
    List<LostPropertyEntity> findAllByPhone(String phone);
}
