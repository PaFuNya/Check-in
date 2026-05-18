package org.example.repository;

import org.example.enitity.LostRegisterEntity;

import java.util.List;

public interface LostRegisterRepository extends  BaseRepository<LostRegisterEntity>{
    /**
     * 根据手机号查询丢失登记信息
     * @param phone
     * @return
     */
    List<LostRegisterEntity> findAllByPhone(String phone);
}
