package net.runelite.client.plugins.microbot.viruzosbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.concurrent.CompletionException;

public class ViruzBotControl
{
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ViruzBotControl.class);

	private static final String TOKEN = "MTQ5Mzc3NDgyMTEyMzk1Mjc4MA.GOmxif.3vfWdqipwy04eP-qmTo3SLlTzjqYJ_sGf8s7WU";
	private static final String CHANNEL_NAME = "viruzffabot";
	private static final String ROLE_NAME = "SO";

	private final ViruzFFABotScript script;
	private DiscordApi api;

	public ViruzBotControl(ViruzFFABotScript script)
	{
		this.script = script;
	}

	public void start()
	{
		new DiscordApiBuilder().setToken(TOKEN).login().thenAccept(connected -> {
			this.api = connected;
			connected.addMessageCreateListener(this::onMessage);
			log.info("ViruzBotControl connected to Discord");
		}).exceptionally(e -> {
			log.info("ViruzBotControl failed to connect: {}", e.getMessage());
			return null;
		});
	}

	public void shutdown()
	{
		if (api != null)
		{
			try
			{
				api.disconnect();
			}
			catch (CompletionException e)
			{
				log.info("ViruzBotControl disconnect error: {}", e.getMessage());
			}
			api = null;
			log.info("ViruzBotControl disconnected");
		}
	}

	private void onMessage(MessageCreateEvent event)
	{
		try
		{
			Message message = event.getMessage();
			TextChannel channel = message.getChannel().asTextChannel().orElse(null);
			if (channel == null || !channel.getName().equalsIgnoreCase(CHANNEL_NAME)) return;

			User author = message.getAuthor().asUser().orElse(null);
			if (author == null) return;

			boolean hasRole = author.getRoles(channel.getServer().orElse(null)).stream()
					.anyMatch(role -> role.getName().equalsIgnoreCase(ROLE_NAME));
			if (!hasRole) return;

			String content = message.getContent().trim();
			if (!content.startsWith("!")) return;

			script.handleDiscordCommand(content);
		}
		catch (Exception e)
		{
			log.info("Error handling Discord command: {}", e.getMessage());
		}
	}
}
