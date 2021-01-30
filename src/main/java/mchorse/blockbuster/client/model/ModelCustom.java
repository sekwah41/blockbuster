package mchorse.blockbuster.client.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mchorse.blockbuster.api.Model;
import mchorse.blockbuster.api.ModelLimb.Holding;
import mchorse.blockbuster.api.ModelPose;
import mchorse.blockbuster.api.ModelTransform;
import mchorse.blockbuster.api.formats.obj.ShapeKey;
import mchorse.blockbuster.utils.EntityUtils;
import mchorse.blockbuster_pack.morphs.CustomMorph;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/**
 * Custom Model class
 *
 * This class is responsible for managing available custom models that have
 * been loaded from config folder or from server and also render a custom model
 * itself.
 */
@SideOnly(Side.CLIENT)
public class ModelCustom extends ModelBiped
{
    /**
     * Repository of custom models that are available for usage
     */
    public static final Map<String, ModelCustom> MODELS = new HashMap<String, ModelCustom>();

    /**
     * Model data
     */
    public Model model;

    /**
     * Current pose
     */
    public ModelPose pose;

    public CustomMorph current;

    /**
     * Array of all limbs that has been parsed from JSON model
     */
    public ModelCustomRenderer[] limbs;

    /**
     * Array of limbs that has to be rendered (child limbs doesn't have to
     * be rendered, because they're getting render call from parent).
     */
    public ModelCustomRenderer[] renderable;

    public ModelCustomRenderer[] left;
    public ModelCustomRenderer[] right;
    public ModelCustomRenderer[] armor;

    public Map<String, ResourceLocation> materials;
    public List<ShapeKey> shapes;

    /**
     * Initiate the model with the size of the texture
     */
    public ModelCustom(Model model)
    {
        this.model = model;
        this.textureWidth = model.texture[0];
        this.textureHeight = model.texture[1];
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        boolean keying = this.current != null && this.current.keying;

        GlStateManager.enableBlend();

        if (keying)
        {
            GlStateManager.glBlendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT);
            GlStateManager.blendFunc(GL11.GL_ZERO, GL11.GL_ZERO);
        }
        else
        {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }

        for (ModelRenderer limb : this.renderable)
        {
            limb.render(scale);
        }

        if (keying)
        {
            GlStateManager.glBlendEquation(GL14.GL_FUNC_ADD);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }

        GlStateManager.disableBlend();

        this.current = null;
    }

    public void renderForStencil(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        for (int i = 0; i < this.limbs.length; i ++)
        {
            this.limbs[i].setupStencilRendering(i + 1);
        }

        for (ModelRenderer limb : this.renderable)
        {
            limb.render(scale);
        }

        GlStateManager.disableBlend();
    }

    /**
     * Set hands postures
     */
    public void setHands(EntityLivingBase entity)
    {
        ItemStack rightItem = entity.getHeldItemMainhand();
        ItemStack leftItem = entity.getHeldItemOffhand();

        ModelBiped.ArmPose right = ModelBiped.ArmPose.EMPTY;
        ModelBiped.ArmPose left = ModelBiped.ArmPose.EMPTY;

        if (!rightItem.isEmpty())
        {
            right = ModelBiped.ArmPose.ITEM;

            if (entity.getItemInUseCount() > 0)
            {
                EnumAction enumaction = rightItem.getItemUseAction();

                if (enumaction == EnumAction.BLOCK)
                {
                    right = ModelBiped.ArmPose.BLOCK;
                }
                else if (enumaction == EnumAction.BOW)
                {
                    right = ModelBiped.ArmPose.BOW_AND_ARROW;
                }
            }
        }

        if (!leftItem.isEmpty())
        {
            left = ModelBiped.ArmPose.ITEM;

            if (entity.getItemInUseCount() > 0)
            {
                EnumAction enumaction1 = leftItem.getItemUseAction();

                if (enumaction1 == EnumAction.BLOCK)
                {
                    left = ModelBiped.ArmPose.BLOCK;
                }
            }
        }

        this.rightArmPose = right;
        this.leftArmPose = left;
    }

    /**
     * This method is responsible for setting gameplay wise features like head
     * looking, idle rotating (like arm swinging), swinging and swiping
     */
    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn)
    {
        if (entityIn instanceof EntityLivingBase)
        {
            this.setHands((EntityLivingBase) entityIn);
        }
        else
        {
            this.leftArmPose = ArmPose.EMPTY;
            this.rightArmPose = ArmPose.EMPTY;
        }

        for (ModelCustomRenderer limb : this.limbs)
        {
            boolean mirror = limb.limb.mirror;
            boolean invert = limb.limb.invert;

            if (limb instanceof ModelOBJRenderer)
            {
                ModelOBJRenderer obj = (ModelOBJRenderer) limb;

                obj.materials = this.materials;
                obj.shapes = this.shapes;
            }

            float factor = mirror ^ invert ? -1 : 1;
            float PI = (float) Math.PI;

            /* Reseting the angles */
            this.applyLimbPose(limb);

            if ((limb.limb.lookX || limb.limb.lookY) && !limb.limb.wheel)
            {
                if (limb.limb.lookX)
                {
                    limb.rotateAngleX += headPitch * 0.017453292F;
                }

                if (limb.limb.lookY)
                {
                    if (invert)
                    {
                        limb.rotateAngleZ += netHeadYaw * 0.017453292F;
                    }
                    else
                    {
                        limb.rotateAngleY += netHeadYaw * 0.017453292F;
                    }
                }
            }

            if (limb.limb.swinging)
            {
                boolean flag = entityIn instanceof EntityLivingBase && ((EntityLivingBase) entityIn).getTicksElytraFlying() > 4;
                float f = 1.0F;

                if (flag)
                {
                    f = (float) (entityIn.motionX * entityIn.motionX + entityIn.motionY * entityIn.motionY + entityIn.motionZ * entityIn.motionZ);
                    f = f / 0.2F;
                    f = f * f * f;
                }

                if (f < 1.0F)
                {
                    f = 1.0F;
                }

                float f2 = mirror ^ invert ? 1 : 0;
                float f3 = limb.limb.holding == Holding.NONE ? 1.4F : 1.0F;

                limb.rotateAngleX += MathHelper.cos(limbSwing * 0.6662F + PI * f2) * f3 * limbSwingAmount / f;
            }

            if (limb.limb.idle)
            {
                limb.rotateAngleZ += (MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F) * factor;
                limb.rotateAngleX += (MathHelper.sin(ageInTicks * 0.067F) * 0.05F) * factor;
            }

            if (limb.limb.swiping && !limb.limb.wing && this.swingProgress > 0.0F)
            {
                float swing = this.swingProgress;
                float bodyY = MathHelper.sin(MathHelper.sqrt(swing) * PI * 2F) * 0.2F;

                swing = 1.0F - swing;
                swing = swing * swing * swing;
                swing = 1.0F - swing;

                float sinSwing = MathHelper.sin(swing * PI);
                float sinSwing2 = MathHelper.sin(this.swingProgress * PI) * -(0.0F - 0.7F) * 0.75F;

                limb.rotateAngleX = limb.rotateAngleX - (sinSwing * 1.2F + sinSwing2);
                limb.rotateAngleY += bodyY * 2.0F * factor;
                limb.rotateAngleZ += MathHelper.sin(this.swingProgress * PI) * -0.4F * factor;
            }

            if (limb.limb.holding != Holding.NONE)
            {
                boolean right = limb.limb.holding == Holding.RIGHT;
                ModelBiped.ArmPose pose = right ? this.rightArmPose : this.leftArmPose;
                ModelBiped.ArmPose opposite = right ? this.leftArmPose : this.rightArmPose;

                switch (pose)
                {
                    case BLOCK:
                        limb.rotateAngleX = limb.rotateAngleX * 0.5F - 0.9424779F;
                        limb.rotateAngleY = 0.5235988F * (right ? -1 : 1);
                    break;

                    case ITEM:
                        if (limb.limb.hold)
                        {
                            limb.rotateAngleX = limb.rotateAngleX * 0.5F - PI / 10F;
                        }
                    break;
                }

                float rotateAngleX = headPitch * 0.017453292F;
                float rotateAngleY = netHeadYaw * 0.017453292F;

                if (right && pose == ModelBiped.ArmPose.BOW_AND_ARROW)
                {
                    limb.rotateAngleY = -0.1F + rotateAngleY - 0.4F;
                    limb.rotateAngleY = 0.1F + rotateAngleY;
                    limb.rotateAngleX = -((float) Math.PI / 2F) + rotateAngleX;
                    limb.rotateAngleX = -((float) Math.PI / 2F) + rotateAngleX;
                }
                else if (!right && opposite == ModelBiped.ArmPose.BOW_AND_ARROW)
                {
                    limb.rotateAngleY = -0.1F + rotateAngleY;
                    limb.rotateAngleY = 0.1F + rotateAngleY + 0.4F;
                    limb.rotateAngleX = -((float) Math.PI / 2F) + rotateAngleX;
                    limb.rotateAngleX = -((float) Math.PI / 2F) + rotateAngleX;
                }
            }

            if (limb.limb.wheel)
            {
                limb.rotateAngleX += limbSwing * factor;

                if (limb.limb.lookY)
                {
                    limb.rotateAngleY = netHeadYaw / 180 * (float) Math.PI;
                }
            }

            if (limb.limb.wing)
            {
                float wingFactor = MathHelper.cos(ageInTicks * 1.3F) * (float) Math.PI * 0.25F * (0.5F + limbSwingAmount) * factor;

                if (limb.limb.swiping)
                {
                    limb.rotateAngleZ = wingFactor;
                }
                else
                {
                    limb.rotateAngleY = wingFactor;
                }
            }

            if (limb.limb.roll)
            {
                limb.rotateAngleZ += -EntityUtils.getRoll(entityIn, ageInTicks % 1) / 180F * PI;
            }
        }
    }

    /**
     * Apply transform from current pose on given limb
     */
    public void applyLimbPose(ModelCustomRenderer limb)
    {
        ModelTransform trans = this.pose.limbs.get(limb.limb.name);

        limb.applyTransform(trans == null ? ModelTransform.DEFAULT : trans);
    }

    /**
     * Get renderer for an arm
     */
    public ModelCustomRenderer[] getRenderForArm(EnumHandSide side)
    {
        if (side == EnumHandSide.LEFT)
        {
            return this.left;
        }
        else if (side == EnumHandSide.RIGHT)
        {
            return this.right;
        }

        return null;
    }

    /**
     * Clean up resources used by this model 
     */
    public void delete()
    {
        for (ModelCustomRenderer renderer : this.limbs)
        {
            renderer.delete();
        }
    }
}