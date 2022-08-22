/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.api.process;

/**
 * Subprocesses only run while other processes are running. Since they interrupt processes.
 *
 * For now this means, only while pathing, or some other task.
 */
public interface IBaritoneSubprocess extends IBaritoneProcess {

    /**
     * Interruption requires a subprocess to have a higher priority than regular processes.
     *
     * We just add 1, so it is just barely higher than the default process priority.
     * @return Defaults to default priority + 1
     */
    @Override
    default double priority() {
        return IBaritoneProcess.super.priority() + 1;
    }

    @Override
    default boolean isTemporary() {
        return true;
    }

    @Override
    default boolean isSubprocess() {
        return true;
    }
}
