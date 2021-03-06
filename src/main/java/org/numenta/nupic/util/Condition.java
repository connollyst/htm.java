/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic.util;

/**
 * Implemented to be used as arguments in other operations.
 * see {@link ArrayUtils#retainLogicalAnd(int[], Condition[])};
 * {@link ArrayUtils#retainLogicalAnd(double[], Condition[])}.
 */
public interface Condition<T> {
    /**
     * Convenience adapter to remove verbosity
     * @author metaware
     *
     */
    public class Adapter<T> implements Condition<T> {
        public boolean eval(int n) { return false; }
        public boolean eval(double d) { return false; }
        public boolean eval(T t) { return false; }
    }
    public boolean eval(int n);
    public boolean eval(double d);
    public boolean eval(T t);
}