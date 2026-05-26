package ru.LevLezhnin.NauJava.model;

/**
 * Роли пользователей в системе File Bridge.
 * <ul>
 *   <li>{@link #USER} - обычный пользователь с ограниченными правами</li>
 *   <li>{@link #ADMIN} - администратор с полными правами управления</li>
 * </ul>
 *
 * @author Лев Лежнин
 */
public enum UserRole {
    USER,
    ADMIN
}
