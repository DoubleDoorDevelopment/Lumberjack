/*
 * Copyright (c) 2014-2016, Dries007 & DoubleDoorDevelopment
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of DoubleDoorDevelopment nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package net.doubledoordev.lumberjack.util;

import java.util.List;

/**
 * @author Dries007
 */
public class Constants
{
    public static final String MODID = "lumberjack";
    public static final String MOD_NAME = "LumberJack";
    public static final String VERSION = "1.4.1";

    public static final class TableData
    {
        public String header;
        public List<String> strings;
        private int width;

        public TableData(String header, List<String> data)
        {
            this.header = header;
            this.strings = data;
            width = header.length();

            updateWidth();
        }

        private void updateWidth()
        {
            for (String string : strings) if (width < string.length()) width = string.length();
        }
    }

    public static String makeTable(TableData... datas)
    {
        int size = 0;
        for (TableData data : datas) size += data.width * data.strings.size();
        StringBuilder stringBuilder = new StringBuilder(size);

        for (TableData data : datas)
            stringBuilder.append('|').append(' ').append(data.header).append(new String(new char[data.width - data.header.length() + 1]).replace('\0', ' '));
        stringBuilder.append('|').append('\n');
        for (TableData data : datas)
            stringBuilder.append('+').append(new String(new char[data.width + 2]).replace('\0', '-'));
        stringBuilder.append('+').append('\n');
        int i = 0;
        while (i < datas[0].strings.size())
        {
            for (TableData data : datas)
                stringBuilder.append('|').append(' ').append(data.strings.get(i)).append(new String(new char[data.width - data.strings.get(i).length() + 1]).replace('\0', ' '));
            stringBuilder.append('|').append('\n');
            i++;
        }

        return stringBuilder.toString();
    }
}
