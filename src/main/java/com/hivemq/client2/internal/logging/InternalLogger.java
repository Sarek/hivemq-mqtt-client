/*
 * Copyright 2018-present HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.client2.internal.logging;

import org.jetbrains.annotations.NotNull;

/**
 * @author Silvio Giebl
 */
public interface InternalLogger {

    void error(@NotNull String message);

    void error(@NotNull String message, @NotNull Throwable throwable);

    void error(@NotNull String format, @NotNull Object arg);

    void error(@NotNull String format, @NotNull Object arg1, @NotNull Object arg2);

    void warn(@NotNull String message);

    void warn(@NotNull String format, @NotNull Object arg);

    void warn(@NotNull String format, @NotNull Object arg1, @NotNull Object arg2);

    void trace(@NotNull String message);

    void trace(@NotNull String format, @NotNull Object arg);

    void trace(@NotNull String format, @NotNull Object arg1, @NotNull Object arg2);
}
