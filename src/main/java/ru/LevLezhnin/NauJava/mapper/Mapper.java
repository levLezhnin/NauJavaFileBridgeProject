package ru.LevLezhnin.NauJava.mapper;

/**
 * Общий интерфейс мапперов между сущностями и DTO.
 *
 * @param <FromType> исходный тип
 * @param <ToType> целевой тип
 * @author Лев Лежнин
 */
public interface Mapper<FromType, ToType> {
    ToType map(FromType object);
}
