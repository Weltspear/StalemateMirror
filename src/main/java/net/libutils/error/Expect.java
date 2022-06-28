/*
 * Stalemate Game
 * Copyright (C) 2022 Weltspear
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.libutils.error;

import org.jetbrains.annotations.NotNull;

public class Expect<T, R extends ErrorResult> {
    private final T some;
    private final R result;

    public Expect(@NotNull R result){
        this.some = null;
        this.result = result;
    }

    public Expect(@NotNull T some){
        this.some = some;
        this.result = null;
    }

    public T unwrap() {
        if (some == null){
            throw new NullPointerException("Unable to unwrap null value");
        }
        else{
            return some;
        }
    }

    /***
     * Gets ErrorResult
     */
    public R getResult(){
        if (result == null){
            throw new NullPointerException("result is null");
        }
        else {
            return result;
        }
    }

    public boolean isNone(){
        return some == null;
    }

}

