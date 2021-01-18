package mchorse.blockbuster.commands.action;

import mchorse.blockbuster.CommonProxy;
import mchorse.blockbuster.recording.data.Mode;
import mchorse.blockbuster.recording.scene.Scene;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/**
 * Sub-command /action record
 *
 * This sub-command is responsible for starting recording given filename'd
 * action with optionally provided scene.
 */
public class SubCommandActionRecord extends CommandBase
{
    @Override
    public String getName()
    {
        return "record";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "blockbuster.commands.action.record";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 1)
        {
            throw new WrongUsageException(this.getUsage(sender));
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);

        if (args.length >= 2)
        {
            Scene scene = CommonProxy.scenes.get(args[1], sender.getEntityWorld());

            if (scene != null)
            {
                CommonProxy.scenes.record(args[1], args[0], player);
            }
        }
        else
        {
            CommonProxy.manager.record(args[0], player, Mode.ACTIONS, true, true, null);
        }
    }
}