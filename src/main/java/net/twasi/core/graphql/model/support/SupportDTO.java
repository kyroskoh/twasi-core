package net.twasi.core.graphql.model.support;

import net.twasi.core.database.models.User;
import net.twasi.core.database.models.UserRank;
import net.twasi.core.database.models.support.SupportTicket;
import net.twasi.core.database.models.support.SupportTicketMessage;
import net.twasi.core.database.models.support.SupportTicketType;
import net.twasi.core.database.repositories.SupportTicketRepository;
import net.twasi.core.graphql.TwasiGraphQLHandledException;
import net.twasi.core.graphql.model.GraphQLPagination;
import net.twasi.core.services.providers.DataService;
import net.twasi.core.services.providers.TelegramService;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.stream.Collectors;

public class SupportDTO {

    private User user;
    private SupportTicketRepository repository;
    private int paginationAmount = 10; // TODO add to config

    public SupportDTO(User user) {
        repository = DataService.get().get(SupportTicketRepository.class);
        this.user = user;
    }

    public GraphQLPagination<SupportTicketDTO> getMyTickets(boolean open) {
        return new GraphQLPagination<>(
                () -> repository.countByUser(user),
                (pg) -> repository.getByUser(user, paginationAmount, pg, open).stream()
                        .map(SupportTicketDTO::new)
                        .collect(Collectors.toList())
        );
    }

    public GraphQLPagination<SupportTicketDTO> getMyClosedTickets() {
        return getMyTickets(false);
    }

    public GraphQLPagination<SupportTicketDTO> getMyOpenTickets() {
        return getMyTickets(true);
    }

    public GraphQLPagination<SupportTicketDTO> getAdminTickets(boolean open) {
        if (!user.getRank().equals(UserRank.TEAM)) {
            return null;
        }

        return new GraphQLPagination<>(
                () -> repository.countAll(open),
                (pg) -> repository.getAll(paginationAmount, pg, open).stream()
                        .map(SupportTicketDTO::new)
                        .collect(Collectors.toList())
        );
    }

    public GraphQLPagination<SupportTicketDTO> getOpenAdminTickets() {
        return getAdminTickets(true);
    }

    public GraphQLPagination<SupportTicketDTO> getClosedAdminTickets() {
        return getAdminTickets(false);
    }

    public SupportTicketDTO create(String topic, String message, String category) {
        SupportTicketType categoryType;
        try {
            categoryType = SupportTicketType.valueOf(category);
        } catch (IllegalArgumentException e) {
            throw new TwasiGraphQLHandledException("This category type is not available.", "support.validation.invalidCategory");
        }

        topic = topic.trim();
        if (topic.isEmpty()) {
            throw new TwasiGraphQLHandledException("The topic may not be empty.", "support.validation.emptyTopic");
        }

        message = message.trim();
        if (message.isEmpty()) {
            throw new TwasiGraphQLHandledException("The message may not be empty.", "support.validation.emptyMessage");
        }

        SupportTicket ticket = repository.create(user, topic, message, categoryType);
        List<SupportTicketMessage> messages = ticket.getEntries();
        SupportTicketMessage initial = ticket.getEntries().get(0);

        TelegramService telegram = TelegramService.get();
        if (telegram.isConnected()) {
            try {
                telegram.sendMessageToTelegramChat(
                        "(☞ﾟヮﾟ)☞ Neues Supportticket: #" +
                                ticket.getId() + ", User: " + ticket.getOwner().getTwitchAccount().getDisplayName() + ", Bereich: " + ticket.getCategory() + ", " + ticket.getTopic() + " - " + initial.getMessage().substring(0, Math.min(initial.getMessage().length(), 100))
                );
            } catch (TelegramApiException ignored) {
            }
        }

        return new SupportTicketDTO(ticket);
    }

    public SupportTicketDTO reply(String id, String message, Boolean close, Boolean isAdminContext) {
        SupportTicket ticket;

        if (isAdminContext) {
            if (user.getRank() != UserRank.TEAM) {
                // Not permitted to post admin response.
                throw new TwasiGraphQLHandledException("You are not permitted to reply in staff mode.", null);
            }
        }

        if (!isAdminContext) {
            // Only search for personal tickets
            ticket = repository.getByUser(user).stream().filter(t -> t.getId().toString().equals(id)).findFirst().orElse(null); // TODO query directly in database
        } else {
            // Search for all tickets, verified admin
            ticket = repository.getById(id);
        }

        // Ticket not found :(
        if (ticket == null) {
            throw new TwasiGraphQLHandledException("The ticket you tried to edit does not exist.", null);
        }

        message = message.trim();

        if (message.isEmpty()) {
            throw new TwasiGraphQLHandledException("The message may not be empty.", "support.validation.emptyMessage");
        }

        repository.addReply(ticket, user, isAdminContext, message, close);

        if (!isAdminContext) {
            TelegramService telegram = TelegramService.get();
            if (telegram.isConnected()) {
                try {
                    telegram.sendMessageToTelegramChat(
                            "(☞ﾟヮﾟ)☞ Neues Antwort auf ein Supportticket: #" +
                                    ticket.getId() + ", User: " + user.getTwitchAccount().getDisplayName() + "; " + message.substring(0, Math.min(message.length(), 100))
                    );
                } catch (TelegramApiException ignored) {
                }
            }
        }

        ticket = repository.getById(ticket.getId());
        return new SupportTicketDTO(ticket);
    }

}
