import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { CornerDownRight, Trash2 } from 'lucide-react'
import type { Comment } from '@/lib/types'
import { timeAgo } from '@/lib/format'
import { Avatar } from '@/components/ui/Avatar'
import { Button } from '@/components/ui/Button'
import { useAuth } from '@/lib/auth'
import { useDeleteComment, usePostComment } from '@/lib/queries'
import { useToast } from '@/components/ui/Toast'
import { errorMessage } from '@/lib/api'
import { EmptyState } from '@/components/ui/EmptyState'

export function CommentThread({ slug, comments }: { slug: string; comments: Comment[] }) {
  const { user } = useAuth()
  const { notify } = useToast()
  const postComment = usePostComment(slug)
  const [draft, setDraft] = useState('')
  const [replyingTo, setReplyingTo] = useState<number | null>(null)
  // Which form is actually posting — `replyingTo` only says which reply box is open, so using it
  // here made every open reply button spin whenever anything was submitted.
  const [submittingFor, setSubmittingFor] = useState<number | null | undefined>(undefined)

  const submit = (event: FormEvent, parentId: number | null, value: string, reset: () => void) => {
    event.preventDefault()
    const body = value.trim()
    if (!body) return
    setSubmittingFor(parentId)
    postComment.mutate(
      { body, parentId },
      {
        onSuccess: () => {
          reset()
          setReplyingTo(null)
        },
        onError: (error) => notify(errorMessage(error), 'error'),
        onSettled: () => setSubmittingFor(undefined),
      },
    )
  }

  return (
    <section className="space-y-4">
      {user ? (
        <form onSubmit={(event) => submit(event, null, draft, () => setDraft(''))} className="panel p-4">
          <div className="flex gap-3">
            <Avatar user={user} size="sm" />
            <textarea
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              rows={2}
              maxLength={1500}
              placeholder="Add to the discussion…"
              className="min-h-[2.5rem] w-full resize-y rounded-lg border border-ink-700 bg-ink-900 px-3 py-2 text-sm text-mist-100 placeholder:text-mist-500 focus:border-flare-500 focus:outline-none"
            />
          </div>
          <div className="mt-2.5 flex justify-end">
            <Button
              type="submit"
              size="sm"
              variant="primary"
              disabled={!draft.trim()}
              loading={postComment.isPending && submittingFor === null}
            >
              Post
            </Button>
          </div>
        </form>
      ) : (
        <div className="panel px-4 py-3 text-sm text-mist-400">
          <Link to="/login" className="text-flare-400 hover:underline">
            Sign in
          </Link>{' '}
          to join the discussion.
        </div>
      )}

      {comments.length === 0 ? (
        <EmptyState title="No comments yet" hint="Be the first to say something about this look." />
      ) : (
        <ul className="space-y-3">
          {comments.map((comment) => (
            <li key={comment.id}>
              <CommentRow
                comment={comment}
                slug={slug}
                canReply={Boolean(user)}
                replying={replyingTo === comment.id}
                onToggleReply={() =>
                  setReplyingTo((current) => (current === comment.id ? null : comment.id))
                }
                onSubmitReply={(event, value, reset) =>
                  submit(event, comment.id, value, reset)
                }
                submitting={postComment.isPending && submittingFor === comment.id}
              />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

interface CommentRowProps {
  comment: Comment
  slug: string
  canReply: boolean
  replying: boolean
  onToggleReply: () => void
  onSubmitReply: (event: FormEvent, value: string, reset: () => void) => void
  submitting: boolean
}

function CommentRow({
  comment,
  slug,
  canReply,
  replying,
  onToggleReply,
  onSubmitReply,
  submitting,
}: CommentRowProps) {
  const { notify } = useToast()
  const deleteComment = useDeleteComment(slug)
  const [reply, setReply] = useState('')

  const remove = () => {
    if (!window.confirm('Delete this comment?')) return
    deleteComment.mutate(comment.id, {
      onError: (error) => notify(errorMessage(error), 'error'),
    })
  }

  return (
    <div className="panel p-4">
      <div className="flex gap-3">
        <Avatar user={comment.author} size="sm" />
        <div className="min-w-0 flex-1">
          <div className="flex items-baseline gap-2">
            <Link
              to={`/u/${comment.author.username}`}
              className="text-sm font-medium text-mist-100 transition-colors hover:text-flare-400"
            >
              {comment.author.displayName}
            </Link>
            <span className="text-xs text-mist-500">{timeAgo(comment.createdAt)}</span>
          </div>
          <p className="mt-1 whitespace-pre-line text-sm leading-relaxed text-mist-300">
            {comment.body}
          </p>

          <div className="mt-2 flex items-center gap-3 text-xs">
            {canReply && (
              <button
                type="button"
                onClick={onToggleReply}
                className="text-mist-500 transition-colors hover:text-mist-200"
              >
                Reply
              </button>
            )}
            {comment.editable && (
              <button
                type="button"
                onClick={remove}
                className="inline-flex items-center gap-1 text-mist-500 transition-colors hover:text-flare-400"
              >
                <Trash2 size={12} /> Delete
              </button>
            )}
          </div>

          {replying && (
            <form
              onSubmit={(event) => onSubmitReply(event, reply, () => setReply(''))}
              className="mt-3 flex items-start gap-2"
            >
              <textarea
                autoFocus
                value={reply}
                onChange={(event) => setReply(event.target.value)}
                rows={2}
                maxLength={1500}
                placeholder={`Reply to ${comment.author.displayName}…`}
                className="w-full resize-y rounded-lg border border-ink-700 bg-ink-900 px-3 py-2 text-sm text-mist-100 placeholder:text-mist-500 focus:border-flare-500 focus:outline-none"
              />
              <Button type="submit" size="sm" disabled={!reply.trim()} loading={submitting}>
                Send
              </Button>
            </form>
          )}
        </div>
      </div>

      {comment.replies.length > 0 && (
        <ul className="mt-3 space-y-3 border-l border-ink-700 pl-4 sm:pl-6">
          {comment.replies.map((child) => (
            <li key={child.id} className="flex gap-2.5">
              <CornerDownRight size={14} className="mt-1 shrink-0 text-mist-600" />
              <div className="min-w-0 flex-1">
                <div className="flex items-baseline gap-2">
                  <Link
                    to={`/u/${child.author.username}`}
                    className="text-sm font-medium text-mist-200 transition-colors hover:text-flare-400"
                  >
                    {child.author.displayName}
                  </Link>
                  <span className="text-xs text-mist-500">{timeAgo(child.createdAt)}</span>
                </div>
                <p className="mt-0.5 whitespace-pre-line text-sm leading-relaxed text-mist-400">
                  {child.body}
                </p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
