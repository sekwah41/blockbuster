package mchorse.blockbuster.commands.model;

import mchorse.blockbuster.Blockbuster;
import mchorse.blockbuster.CommonProxy;
import mchorse.blockbuster.api.Model;
import mchorse.blockbuster.api.ModelPose;
import mchorse.blockbuster.client.model.ModelCustom;
import mchorse.blockbuster.client.model.parsing.ModelExporterOBJ;
import mchorse.blockbuster.commands.BBCommandBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SubCommandModelExportObj extends BBCommandBase
{
    @Override
    public String getName()
    {
        return "export_obj";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "blockbuster.commands.model.export_obj";
    }

    @Override
    public String getSyntax()
    {
        return "{l}{6}/{r}model {8}export_obj{r} {7}<model_name> [pose]{r}";
    }

    @Override
    public int getRequiredArgs()
    {
        return 1;
    }

    @Override
    public void executeCommand(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        String modelName = args[0];
        ModelCustom model = ModelCustom.MODELS.get(modelName);

        if (model == null)
        {
            throw new CommandException("blockbuster.error.model.export.no_model", modelName);
        }

        Model data = model.model;
        ModelPose pose = args.length >= 2 ? data.getPose(args[1]) : data.getPose("standing");
        String obj = new ModelExporterOBJ(data, pose).export(modelName);

        /* Save */
        String filename = modelName.replaceAll("[^\\w\\d_-]", "_");
        File destination = new File(CommonProxy.configFile, "export/" + filename + ".obj");

        if (data.defaultTexture != null)
        {
            try
            {
                String mtl = "# MTL generated by Blockbuster (version " + Blockbuster.VERSION + ")\n\nnewmtl default\nKd 1.000000 1.000000 1.000000\nNi 1.000000\nd 1.000000\nillum 2\nmap_Kd " + filename + ".png";
                File mtlFile = new File(CommonProxy.configFile, "export/" + filename + ".mtl");
                FileUtils.writeStringToFile(mtlFile, mtl, StandardCharsets.UTF_8);
            }
            catch (Exception e)
            {}

            try
            {
                IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(data.defaultTexture);
                BufferedImage image = TextureUtil.readBufferedImage(resource.getInputStream());
                File texture = new File(CommonProxy.configFile, "export/" + filename + ".png");

                ImageIO.write(image, "png", texture);
            }
            catch (Exception e)
            {}
        }

        try
        {
            FileUtils.writeStringToFile(destination, obj, StandardCharsets.UTF_8);

            Blockbuster.l10n.success(sender, "model.export.obj", modelName);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Blockbuster.l10n.error(sender, "model.export.obj", modelName);
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos)
    {
        if (args.length == 1)
        {
            return getListOfStringsMatchingLastWord(args, ModelCustom.MODELS.keySet());
        }

        return super.getTabCompletions(server, sender, args, pos);
    }
}