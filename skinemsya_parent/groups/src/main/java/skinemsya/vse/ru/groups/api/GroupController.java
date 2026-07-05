package skinemsya.vse.ru.groups.api;

import jakarta.validation.Valid;
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
import skinemsya.vse.ru.common.api.PageRequest;
import skinemsya.vse.ru.common.api.PageResult;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.security.AuthenticatedUser;
import skinemsya.vse.ru.groups.api.dto.AddGroupMemberRequest;
import skinemsya.vse.ru.groups.api.dto.ChatLinkedGroupRequest;
import skinemsya.vse.ru.groups.api.dto.CreateStandaloneGroupRequest;
import skinemsya.vse.ru.groups.api.dto.GroupMemberViewResponse;
import skinemsya.vse.ru.groups.api.dto.GroupResponse;
import skinemsya.vse.ru.groups.api.dto.UpdateGroupRequest;
import skinemsya.vse.ru.groups.application.GroupAccessService;
import skinemsya.vse.ru.groups.application.GroupService;
import skinemsya.vse.ru.groups.domain.Group;
import skinemsya.vse.ru.groups.domain.GroupMemberView;
import skinemsya.vse.ru.groups.domain.exception.GroupNotFoundException;
import skinemsya.vse.ru.integrations.application.TelegramInitDataValidator;
import skinemsya.vse.ru.integrations.domain.TelegramInitData;
import skinemsya.vse.ru.users.application.UserService;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;
    private final GroupAccessService groupAccessService;
    private final TelegramInitDataValidator telegramInitDataValidator;
    private final UserService userService;

    public GroupController(
            GroupService groupService,
            GroupAccessService groupAccessService,
            TelegramInitDataValidator telegramInitDataValidator,
            UserService userService) {
        this.groupService = groupService;
        this.groupAccessService = groupAccessService;
        this.telegramInitDataValidator = telegramInitDataValidator;
        this.userService = userService;
    }

    @PostMapping("/standalone")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createStandalone(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateStandaloneGroupRequest request) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(groupService.createStandalone(request.name(), userId));
    }

    @PostMapping("/chat-linked")
    public GroupResponse createChatLinked(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody ChatLinkedGroupRequest request) {
        long userId = requireUserId(authenticatedUser);
        var initData = telegramInitDataValidator.validateWithChat(request.initData());
        requireSameTelegramIdentity(userId, initData);
        var chat = initData.chat()
                .orElseThrow(() -> new DomainException(
                        ErrorCode.DOMAIN_RULE_VIOLATION, "Telegram chat context is missing in init data"));
        return toResponse(groupService.createFromChat(chat.chatId(), chat.title(), userId));
    }

    private void requireSameTelegramIdentity(long userId, TelegramInitData initData) {
        var user = userService
                .findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated"));
        if (user.telegramUserId() != initData.identity().telegramUserId()) {
            throw new DomainException(ErrorCode.AUTHORIZATION_ERROR, "Telegram init data belongs to another user");
        }
    }

    @GetMapping
    public PageResult<GroupResponse> listGroups(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        long userId = requireUserId(authenticatedUser);
        var result = groupService.listForUser(userId, resolvePageRequest(page, size));
        return mapPage(result, GroupController::toResponse);
    }

    @GetMapping("/{groupId}")
    public GroupResponse getGroup(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long groupId) {
        long userId = requireUserId(authenticatedUser);
        groupAccessService.requireMember(groupId, userId);
        var group = groupService.findById(groupId).orElseThrow(GroupNotFoundException::new);
        return toResponse(group);
    }

    @PutMapping("/{groupId}")
    public GroupResponse updateGroup(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long groupId,
            @Valid @RequestBody UpdateGroupRequest request) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(groupService.updateName(groupId, userId, request.name()));
    }

    @GetMapping("/{groupId}/members")
    public PageResult<GroupMemberViewResponse> listMembers(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long groupId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        long userId = requireUserId(authenticatedUser);
        var result = groupService.listMembers(groupId, userId, resolvePageRequest(page, size));
        return mapPage(result, GroupController::toResponse);
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMemberViewResponse addMember(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long groupId,
            @Valid @RequestBody AddGroupMemberRequest request) {
        long userId = requireUserId(authenticatedUser);
        var member = groupService.addMemberByTelegramUsername(groupId, userId, request.telegramUsername());
        return toResponse(member);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long groupId) {
        long userId = requireUserId(authenticatedUser);
        groupService.delete(groupId, userId);
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }

    private static GroupResponse toResponse(Group group) {
        return new GroupResponse(
                group.id(),
                group.name(),
                group.type(),
                group.telegramChatId(),
                group.ownerId(),
                group.createdAt(),
                group.updatedAt());
    }

    private static GroupMemberViewResponse toResponse(GroupMemberView member) {
        return new GroupMemberViewResponse(
                member.id(),
                member.groupId(),
                member.userId(),
                member.role(),
                member.displayName(),
                member.telegramUsername(),
                member.telegramUserId(),
                member.joinedAt());
    }

    private static PageRequest resolvePageRequest(Integer page, Integer size) {
        if (page == null && size == null) {
            return PageRequest.defaults();
        }
        return PageRequest.of(
                page == null ? 0 : page, size == null ? PageRequest.defaults().size() : size);
    }

    private static <T, R> PageResult<R> mapPage(PageResult<T> source, java.util.function.Function<T, R> mapper) {
        return new PageResult<>(
                source.items().stream().map(mapper).toList(), source.page(), source.size(), source.totalElements());
    }
}
