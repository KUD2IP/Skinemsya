package skinemsya.vse.ru.impl.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.pojo.common.PageRequest;
import skinemsya.vse.ru.api.internal.pojo.telegram.TelegramInitData;
import skinemsya.vse.ru.api.internal.security.AuthenticatedUser;
import skinemsya.vse.ru.api.internal.service.groups.GroupAccessService;
import skinemsya.vse.ru.api.internal.service.groups.GroupService;
import skinemsya.vse.ru.api.internal.service.telegram.TelegramBotClient;
import skinemsya.vse.ru.api.internal.service.telegram.TelegramInitDataValidator;
import skinemsya.vse.ru.api.internal.service.users.UserService;
import skinemsya.vse.ru.api.rest.groups.dto.AddGroupMemberRequest;
import skinemsya.vse.ru.api.rest.groups.dto.ChatLinkedGroupRequest;
import skinemsya.vse.ru.api.rest.groups.dto.CreateStandaloneGroupRequest;
import skinemsya.vse.ru.api.rest.groups.dto.GroupMemberViewResponse;
import skinemsya.vse.ru.api.rest.groups.dto.GroupResponse;
import skinemsya.vse.ru.api.rest.groups.dto.InviteLinkResponse;
import skinemsya.vse.ru.api.rest.groups.dto.PageGroupMemberViewResponse;
import skinemsya.vse.ru.api.rest.groups.dto.PageGroupResponse;
import skinemsya.vse.ru.api.rest.groups.dto.UpdateGroupRequest;
import skinemsya.vse.ru.impl.mapper.rest.GroupRestMapper;
import skinemsya.vse.ru.impl.support.util.InviteShareText;
import skinemsya.vse.ru.impl.support.util.TelegramStartParam;

/** REST API групп и их участников. */
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final GroupAccessService groupAccessService;
    private final TelegramInitDataValidator telegramInitDataValidator;
    private final TelegramBotClient telegramBotClient;
    private final UserService userService;
    private final GroupRestMapper groupRestMapper;

    /** Создаёт автономную группу. */
    @PostMapping("/standalone")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createStandalone(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateStandaloneGroupRequest request) {
        long userId = requireUserId(authenticatedUser);
        return groupRestMapper.toResponse(groupService.createStandalone(request.getName(), userId));
    }

    /** Создаёт или находит группу, связанную с Telegram-чатом из initData. */
    @PostMapping("/chat-linked")
    public GroupResponse createChatLinked(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody ChatLinkedGroupRequest request) {
        long userId = requireUserId(authenticatedUser);
        var initData = telegramInitDataValidator.validateWithChat(request.getInitData());
        requireSameTelegramIdentity(userId, initData);
        var chat = initData.getChat()
                .orElseThrow(() -> new DomainException(
                        ErrorCode.DOMAIN_RULE_VIOLATION, "Telegram chat context is missing in init data"));
        return groupRestMapper.toResponse(groupService.createFromChat(chat.getChatId(), chat.getTitle(), userId));
    }

    /** Возвращает страницу групп текущего пользователя. */
    @GetMapping
    public PageGroupResponse listGroups(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        long userId = requireUserId(authenticatedUser);
        var result = groupService.listForUser(userId, resolvePageRequest(page, size));
        return groupRestMapper.toGroupPageResponse(result);
    }

    /** Возвращает группу по идентификатору. */
    @GetMapping("/{groupId}")
    public GroupResponse getGroup(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long groupId) {
        long userId = requireUserId(authenticatedUser);
        groupAccessService.requireMember(groupId, userId);
        var group = groupService
                .findById(groupId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Group not found"));
        return groupRestMapper.toResponse(group);
    }

    /** Переименовывает группу. */
    @PutMapping("/{groupId}")
    public GroupResponse updateGroup(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long groupId,
            @Valid @RequestBody UpdateGroupRequest request) {
        long userId = requireUserId(authenticatedUser);
        return groupRestMapper.toResponse(groupService.updateName(groupId, userId, request.getName()));
    }

    /** Возвращает ссылку-приглашение в группу. */
    @GetMapping("/{groupId}/invite-link")
    public InviteLinkResponse getInviteLink(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long groupId) {
        long userId = requireUserId(authenticatedUser);
        groupAccessService.requireMember(groupId, userId);
        var group = groupService
                .findById(groupId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Group not found"));
        String startParam = TelegramStartParam.forGroupCode(groupService.inviteCode(groupId));
        return new InviteLinkResponse()
                .url(telegramBotClient.buildMiniAppDeepLink(startParam))
                .startParam(startParam)
                .shareText(InviteShareText.forGroup(group.getName()));
    }

    /** Возвращает страницу участников группы. */
    @GetMapping("/{groupId}/members")
    public PageGroupMemberViewResponse listMembers(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long groupId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        long userId = requireUserId(authenticatedUser);
        var result = groupService.listMembers(groupId, userId, resolvePageRequest(page, size));
        return groupRestMapper.toMemberPageResponse(result);
    }

    /** Добавляет участника в группу по имени в Telegram. */
    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMemberViewResponse addMember(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long groupId,
            @Valid @RequestBody AddGroupMemberRequest request) {
        long userId = requireUserId(authenticatedUser);
        var member = groupService.addMemberByTelegramUsername(groupId, userId, request.getTelegramUsername());
        return groupRestMapper.toResponse(member);
    }

    /** Выводит текущего пользователя из группы. */
    @PostMapping("/{groupId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveGroup(@AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long groupId) {
        long userId = requireUserId(authenticatedUser);
        groupService.leave(groupId, userId);
    }

    /** Удаляет участника из группы от имени владельца. */
    @DeleteMapping("/{groupId}/members/{memberUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long groupId,
            @PathVariable long memberUserId) {
        long userId = requireUserId(authenticatedUser);
        groupService.removeMember(groupId, userId, memberUserId);
    }

    /** Помечает группу удалённой от имени владельца. */
    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long groupId) {
        long userId = requireUserId(authenticatedUser);
        groupService.delete(groupId, userId);
    }

    private void requireSameTelegramIdentity(long userId, TelegramInitData initData) {
        var user = userService
                .findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated"));
        if (user.getTelegramUserId() != initData.getIdentity().getTelegramUserId()) {
            throw new DomainException(ErrorCode.AUTHORIZATION_ERROR, "Telegram init data belongs to another user");
        }
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }

    private static PageRequest resolvePageRequest(Integer page, Integer size) {
        if (page == null && size == null) {
            return PageRequest.defaults();
        }
        return PageRequest.of(
                page == null ? 0 : page, size == null ? PageRequest.defaults().size() : size);
    }
}
