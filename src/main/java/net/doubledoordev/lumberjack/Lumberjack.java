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

package net.doubledoordev.lumberjack;

import org.apache.logging.log4j.Logger;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.doubledoordev.lumberjack.util.EventHandler;


@Mod("lumberjack")
public class Lumberjack
{
	static final String MOD_ID = "lumberjack";
	public static Logger LOGGER;

	public Lumberjack()
	{
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, LumberjackConfig.spec);

		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(new EventHandler());
		MinecraftForge.EVENT_BUS.register(new RegistrationHandler());
	}

	private void setup(final FMLCommonSetupEvent event)
	{

	}
	//private String[] banList = ModConfig.banList;

	@Mod.EventBusSubscriber(modid = MOD_ID)
	public static class RegistrationHandler {
//		@SubscribeEvent(priority = EventPriority.LOWEST)
//		public static void registerRecipesEvent(RegistryEvent.Register<IRecipe> event) {
//			for (IRecipe item : recipesList) {
//				event.getRegistry().register(item);
//			}
//		}
	}

//    /**+
//     * Does blacklist matching based on rules explained in the coding comment.
//     * Also prints out the entry that got hit, to aid in debugging faulty configs.
//     */
//    public static boolean isBlacklisted(String name)
//    {
//        // Let's avoid that unpleasantly before it even happens. Cannot print though, since we don't have info.
//        if (Strings.isNullOrEmpty(name)) return true;
//
//        for (String ban : instance.banList)
//        {
//            if (ban.equalsIgnoreCase(name))
//            {
//                logger.info("Material {} is blacklisted. It matches {} literally.", name, ban);
//                return true;
//            }
//            if (ban.charAt(0) == '*' && name.endsWith(ban.substring(1)))
//            {
//                logger.info("Material {} is blacklisted. It matches {} as a suffix.", name, ban);
//                return true;
//            }
//            if (ban.charAt(ban.length() - 1) == '*' && name.startsWith(ban.substring(0, ban.length() - 2)))
//            {
//                logger.info("Material {} is blacklisted. It matches {} as a suffix.", name, ban);
//                return true;
//            }
//        }
//
//        return false;
//    }

}
