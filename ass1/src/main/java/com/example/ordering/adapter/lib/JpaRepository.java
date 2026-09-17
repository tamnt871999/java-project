package com.example.ordering.adapter.lib;

import java.util.Optional;

/**
 * THU VIEN GIA LAP - dong vai lifeline "Spring Data JPA".
 *
 * Chu ky ham cot loi duoc giu giong Spring Data that, de khi ban chuyen bai
 * nay sang Spring Boot thi chi viec xoa package adapter/lib di va doi
 * import sang org.springframework.data.jpa.repository.JpaRepository - cac
 * class con lai gan nhu khong phai sua.
 *
 * Interface nay hoan toan TONG QUAT: no khong he biet Order hay OrderEntity la
 * gi. Dung nhu mot thu vien that.
 */
public interface JpaRepository<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);
}
