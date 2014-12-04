begin transaction
CREATE INDEX ON :Action(type);
CREATE CONSTRAINT ON (action:Action) ASSERT action.name IS UNIQUE;
CREATE CONSTRAINT ON (role:Role) ASSERT role.id IS UNIQUE;
CREATE CONSTRAINT ON (role:Role) ASSERT role.name IS UNIQUE;
CREATE CONSTRAINT ON (application:Application) ASSERT application.id IS UNIQUE;
CREATE CONSTRAINT ON (application:Application) ASSERT application.name IS UNIQUE;
CREATE CONSTRAINT ON (conversation:Conversation) ASSERT conversation.userId IS UNIQUE;
CREATE CONSTRAINT ON (visible:Visible) ASSERT visible.id IS UNIQUE;
CREATE CONSTRAINT ON (conversationMessage:ConversationMessage) ASSERT conversationMessage.id IS UNIQUE;
CREATE CONSTRAINT ON (u:UserBook) ASSERT u.userid IS UNIQUE;
CREATE INDEX ON :ConversationMessage(from);
CREATE INDEX ON :ConversationMessage(to);
CREATE INDEX ON :ConversationMessage(cc);
commit
begin transaction
CREATE (:DeleteGroup:ProfileGroup:Group);
commit

