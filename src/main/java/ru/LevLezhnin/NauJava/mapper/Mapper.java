package ru.LevLezhnin.NauJava.mapper;

public interface Mapper<FromType, ToType> {
    ToType map(FromType object);
}
