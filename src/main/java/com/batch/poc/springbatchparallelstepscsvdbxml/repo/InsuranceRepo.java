package com.batch.poc.springbatchparallelstepscsvdbxml.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.batch.poc.springbatchparallelstepscsvdbxml.entity.Insurance;

@Repository
public interface InsuranceRepo extends CrudRepository<Insurance, Long>{

}
