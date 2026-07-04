# Module: groups

## Purpose

Модуль `groups` управляет группами пользователей: создание, участники, типы групп и минимальная роль владельца.

## Responsibilities

- Создание `CHAT_LINKED`-групп из Telegram-чата.
- Создание `STANDALONE`-групп вручную в Mini App.
- Управление участниками группы.
- Присоединение пользователей из чата при входе в Mini App.
- Ручное добавление участников владельцем standalone-группы.
- Редактирование и удаление группы (только владелец).

## Domain Objects

- `Group` — группа с `id`, `name`, `type` (`CHAT_LINKED` | `STANDALONE`), `telegramChatId` (nullable), `ownerId`.
- `GroupMember` — связь user ↔ group с `role` (`owner` | `member`).
- `GroupType` — enum: `CHAT_LINKED`, `STANDALONE`.

## Dependencies

- `users` — идентификация участников, поиск по Telegram user id.
- `common` — ошибки, идентификаторы.

## Events

- `GroupCreated` — группа создана (chat-linked или standalone).
- `MemberJoined` — участник добавлен в группу.
- `MemberLeft` — участник покинул группу.
- `GroupDeleted` — группа удалена владельцем.

## Database Objects

- `groups` — id, name, type, telegram_chat_id (nullable, unique), owner_id (FK users), created_at, updated_at, deleted_at.
- `group_members` — id, group_id (FK), user_id (FK), role, joined_at. Unique (group_id, user_id).

## Public Contracts

- `GroupService.createFromChat(chatId, chatName, ownerId)` → `Group`
- `GroupService.createStandalone(name, ownerId)` → `Group`
- `GroupService.addMember(groupId, userId)` → `GroupMember`
- `GroupService.addMemberByTelegramId(groupId, telegramUserId)` → `GroupMember`
- `GroupService.findByChatId(telegramChatId)` → `Optional<Group>`
- `GroupService.isMember(groupId, userId)` → boolean
- `GroupService.leave(groupId, userId)` → void
- `GroupService.delete(groupId, ownerId)` → void
- REST: `POST /api/v1/groups`, `GET /api/v1/groups`, `POST /api/v1/groups/{id}/members`

## Use Cases

### Create `CHAT_LINKED` Group

Input:

- `telegramChatId`
- `telegramChatTitle`
- authenticated `userId`

Rules:

1. If group with `telegramChatId` exists, return it and ensure user is member.
2. If not exists, create group with type `CHAT_LINKED`.
3. Creator becomes `owner`.
4. `telegramChatId` must be non-null and unique.
5. User must have valid Telegram auth session.

Errors:

- `GROUP_CHAT_ID_REQUIRED` if chat id missing.
- `GROUP_CHAT_ALREADY_LINKED` if chat id belongs to another non-deleted group.
- `USER_NOT_AUTHENTICATED` if auth context missing.

### Create `STANDALONE` Group

Input:

- group `name`
- authenticated `ownerId`

Rules:

1. Create group with type `STANDALONE`.
2. `telegramChatId` is null.
3. Creator is inserted into `group_members` with role `owner`.
4. Group name must be non-empty and <= 255 characters.

Errors:

- `GROUP_NAME_REQUIRED`
- `GROUP_NAME_TOO_LONG`

### Add Member To Standalone Group

Input:

- `groupId`
- requester `ownerId`
- target user by backend `userId` or Telegram user id

Rules:

1. Requester must be group owner.
2. Target user must exist in `users` (MVP does not create unknown users by username).
3. Duplicate add is idempotent: existing member is returned.
4. Adding members to `CHAT_LINKED` manually is not the default MVP path; chat-linked membership is created on Mini App entry from chat context.

Errors:

- `GROUP_OWNER_REQUIRED`
- `GROUP_MEMBER_NOT_FOUND`
- `GROUP_MEMBER_ALREADY_EXISTS` only if API chooses non-idempotent behavior; recommended behavior is idempotent.

## Authorization Rules

| Operation | Required Access |
| --- | --- |
| View group | Any group member |
| Create standalone group | Authenticated user |
| Create chat-linked group | Authenticated user with valid chat context |
| Edit group name | Owner |
| Add standalone member | Owner |
| Leave group | Member, if no active blocking debts |
| Delete group | Owner, if no active blocking events/debts |

Authorization is checked in `groups` application service. Other modules should call `GroupAccessService.requireMember(groupId, userId)` instead of reading `group_members`.

## Invariants

- Exactly one owner exists at group creation.
- `CHAT_LINKED.telegramChatId` is not null.
- `STANDALONE.telegramChatId` is null.
- `(group_id, user_id)` is unique in `group_members`.
- A deleted group is not returned in normal list/search operations.
- User cannot create event in a group without membership.

## AI Implementation Notes

AI agents implementing group features should not infer membership from Telegram chat state alone. Backend membership exists only after a user authenticates and is inserted into `group_members`.

Do not use Telegram username for member lookup unless a future invite flow explicitly defines it. Username is mutable and not guaranteed unique over time.

## Future Extensions

- Приглашения по ссылке.
- Роли администратор.
- Архив групп.
- Синхронизация участников с Telegram-чатом.
